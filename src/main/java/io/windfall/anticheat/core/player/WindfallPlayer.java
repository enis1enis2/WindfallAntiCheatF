package io.windfall.anticheat.core.player;

import io.windfall.anticheat.core.player.data.ActionData;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WindfallPlayer {
    public enum Pose {
        STANDING, FALL_FLYING, SWIMMING, SLEEPING, SPIN_ATTACK, SNEAKING, DYING, LONG_JUMPING;

        public static Pose fromFabric(EntityPose pose) {
            switch (pose) {
                case FALL_FLYING: return FALL_FLYING;
                case SWIMMING: return SWIMMING;
                case SLEEPING: return SLEEPING;
                case SPIN_ATTACK: return SPIN_ATTACK;
                case CROUCHING: return SNEAKING;
                case LONG_JUMPING: return LONG_JUMPING;
                case DYING: return DYING;
                case STANDING:
                default: return STANDING;
            }
        }
    }

    static final class PositionState {
        final double x, y, z;
        final double lastX, lastY, lastZ;
        final double lastLastX, lastLastY, lastLastZ;
        final double deltaX, deltaY, deltaZ;
        final int tickCount;
        PositionState(double x, double y, double z, double lastX, double lastY, double lastZ,
                      double lastLastX, double lastLastY, double lastLastZ,
                      double deltaX, double deltaY, double deltaZ, int tickCount) {
            this.x = x; this.y = y; this.z = z;
            this.lastX = lastX; this.lastY = lastY; this.lastZ = lastZ;
            this.lastLastX = lastLastX; this.lastLastY = lastLastY; this.lastLastZ = lastLastZ;
            this.deltaX = deltaX; this.deltaY = deltaY; this.deltaZ = deltaZ;
            this.tickCount = tickCount;
        }
    }

    static final class GroundState {
        final boolean onGround;
        final boolean lastOnGround;
        final double groundX, groundY, groundZ;
        GroundState(boolean onGround, boolean lastOnGround, double groundX, double groundY, double groundZ) {
            this.onGround = onGround; this.lastOnGround = lastOnGround;
            this.groundX = groundX; this.groundY = groundY; this.groundZ = groundZ;
        }
    }

    static final class RotationState {
        final float yaw, pitch;
        final float lastYaw, lastPitch;
        RotationState(float yaw, float pitch, float lastYaw, float lastPitch) {
            this.yaw = yaw; this.pitch = pitch; this.lastYaw = lastYaw; this.lastPitch = lastPitch;
        }
    }

    private final UUID uuid;
    private final String name;
    private final ServerPlayerEntity serverPlayer;
    private volatile int protocolVersion;

    private volatile PositionState pos = new PositionState(0,0,0, 0,0,0, 0,0,0, 0,0,0, 0);
    private volatile GroundState ground = new GroundState(false, false, 0,0,0);
    private volatile RotationState rotation = new RotationState(0, 0, 0, 0);

    private volatile double width = 0.6;
    private volatile double height = 1.8;
    private volatile boolean serverOnGround;
    private volatile boolean sprinting;
    private volatile boolean sneaking;
    private volatile boolean flying;
    private volatile boolean swimming;
    private volatile boolean climbing;
    private volatile boolean gliding;
    private volatile double elytraMomentum;
    private volatile int glideStartTick;
    private volatile Pose pose = Pose.STANDING;
    private volatile int transactionPing;
    private volatile int transactionId;
    private volatile double velocityX, velocityY, velocityZ;
    private volatile double serverVelocityX, serverVelocityY, serverVelocityZ;
    private volatile boolean velocityReceived;
    private volatile boolean allowFlight;
    private volatile double teleportX, teleportY, teleportZ;
    private final ConcurrentHashMap<String, Integer> violationLevels = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> buffers = new ConcurrentHashMap<>();
    private volatile int attackCooldown;
    private volatile long lastAttackTime;
    private volatile long joinTime;
    private volatile boolean movedSinceTick;
    private volatile boolean valid = true;
    private volatile boolean alertsEnabled = true;
    private final ActionData actionData = new ActionData(this);
    private volatile io.windfall.anticheat.core.compensation.CompensatedWorld compensatedWorld;
    private volatile boolean respawned;
    private volatile boolean cachedInWater;
    private volatile boolean cachedInLava;
    private volatile boolean cachedOnHoney;
    private volatile double cachedSpeedMultiplier = 1.0;
    private volatile double cachedSlownessMultiplier = 1.0;
    private volatile boolean cachedHasSlowFalling;
    private volatile boolean cachedHasLevitation;
    private volatile double cachedLevitationAmplifier = 1.0;
    private volatile boolean cachedIsFallFlying;
    private volatile boolean cachedHasRiptide;

    public WindfallPlayer(ServerPlayerEntity player) {
        this.uuid = player.getUuid();
        this.name = player.getName().getString();
        this.serverPlayer = player;
        this.protocolVersion = 770; // MC 1.21.5
        this.joinTime = System.currentTimeMillis();
    }

    public double getHeight() {
        switch (pose) {
            case FALL_FLYING: case SWIMMING: case SPIN_ATTACK: return 0.6;
            case SLEEPING: return 0.2;
            case DYING: return 0.0;
            case SNEAKING: case LONG_JUMPING: return 1.5;
            case STANDING: default: return 1.8;
        }
    }

    public double getEyeHeight() {
        switch (pose) {
            case FALL_FLYING: case SWIMMING: case SPIN_ATTACK: return 0.4;
            case SLEEPING: return 0.2;
            case DYING: return 0.0;
            case SNEAKING: case LONG_JUMPING: return 1.27;
            case STANDING: default: return 1.62;
        }
    }

    public double getDeltaX() { return pos.deltaX; }
    public double getDeltaZ() { return pos.deltaZ; }
    public double getHorizontalSpeed() { return Math.sqrt(pos.deltaX * pos.deltaX + pos.deltaZ * pos.deltaZ); }
    public double getVerticalSpeed() { return Math.abs(pos.deltaY); }
    public double getDistanceSq(double x, double y, double z) {
        double dx = this.pos.x - x; double dy = this.pos.y - y; double dz = this.pos.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public void setPosition(double x, double y, double z) {
        PositionState old = this.pos;
        this.pos = new PositionState(x, y, z, old.x, old.y, old.z, old.lastX, old.lastY, old.lastZ,
            x - old.x, y - old.y, z - old.z, old.tickCount + 1);
    }

    public void resetTickState() {
        this.movedSinceTick = false;
        GroundState g = this.ground;
        this.ground = new GroundState(g.onGround, g.onGround, g.groundX, g.groundY, g.groundZ);
        RotationState r = this.rotation;
        this.rotation = new RotationState(r.yaw, r.pitch, r.yaw, r.pitch);
    }

    public void setOnGround(boolean onGround) {
        GroundState old = this.ground;
        double gx = onGround ? pos.x : old.groundX;
        double gy = onGround ? pos.y : old.groundY;
        double gz = onGround ? pos.z : old.groundZ;
        this.ground = new GroundState(onGround, old.onGround, gx, gy, gz);
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public ServerPlayerEntity getServerPlayer() { return serverPlayer; }
    public int getProtocolVersion() { return protocolVersion; }
    public void setProtocolVersion(int v) { this.protocolVersion = v; }

    public double getX() { return pos.x; }
    public double getY() { return pos.y; }
    public double getZ() { return pos.z; }
    public double getLastX() { return pos.lastX; }
    public double getLastY() { return pos.lastY; }
    public double getLastZ() { return pos.lastZ; }
    public double getLastLastX() { return pos.lastLastX; }
    public double getLastLastY() { return pos.lastLastY; }
    public double getLastLastZ() { return pos.lastLastZ; }
    public double getDeltaY() { return pos.deltaY; }
    public int getTickCount() { return pos.tickCount; }
    public double getWidth() { return width; }
    public void setHeight(double h) { this.height = h; }
    public boolean isOnGround() { return ground.onGround; }
    public boolean isLastOnGround() { return ground.lastOnGround; }
    public boolean isServerOnGround() { return serverOnGround; }
    public void setServerOnGround(boolean g) { this.serverOnGround = g; }
    public double getGroundX() { return ground.groundX; }
    public double getGroundY() { return ground.groundY; }
    public double getGroundZ() { return ground.groundZ; }
    public boolean isSprinting() { return sprinting; }
    public void setSprinting(boolean s) { this.sprinting = s; }
    public boolean isSneaking() { return sneaking; }
    public synchronized void setSneaking(boolean s) {
        this.sneaking = s;
        if (s && pose == Pose.STANDING) pose = Pose.SNEAKING;
        else if (!s && pose == Pose.SNEAKING) pose = Pose.STANDING;
    }
    public boolean isFlying() { return flying; }
    public void setFlying(boolean f) { this.flying = f; }
    public boolean isSwimming() { return swimming; }
    public synchronized void setSwimming(boolean s) {
        this.swimming = s;
        if (s) pose = Pose.SWIMMING;
        else if (pose == Pose.SWIMMING) pose = Pose.STANDING;
    }
    public boolean isClimbing() { return climbing; }
    public void setClimbing(boolean c) { this.climbing = c; }
    public boolean isGliding() { return gliding; }
    public synchronized void setGliding(boolean g) {
        this.gliding = g;
        if (g) pose = Pose.FALL_FLYING;
        else if (pose == Pose.FALL_FLYING) pose = Pose.STANDING;
    }
    public double getElytraMomentum() { return elytraMomentum; }
    public void setElytraMomentum(double m) { this.elytraMomentum = m; }
    public int getGlideStartTick() { return glideStartTick; }
    public void setGlideStartTick(int t) { this.glideStartTick = t; }
    public Pose getPose() { return pose; }
    public void setPose(Pose p) {
        this.pose = p;
        this.sneaking = (p == Pose.SNEAKING);
        this.swimming = (p == Pose.SWIMMING);
        this.gliding = (p == Pose.FALL_FLYING);
    }
    public boolean isLongJumping() { return pose == Pose.LONG_JUMPING; }
    public void setLongJumping(boolean l) {
        if (l) pose = Pose.LONG_JUMPING;
        else if (pose == Pose.LONG_JUMPING) pose = Pose.STANDING;
    }
    public boolean isDying() { return pose == Pose.DYING; }
    public int getTransactionPing() { return transactionPing; }
    public void setTransactionPing(int p) { this.transactionPing = p; }
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int id) { this.transactionId = id; }
    public double getVelocityX() { return velocityX; }
    public void setVelocityX(double v) { this.velocityX = v; }
    public double getVelocityY() { return velocityY; }
    public void setVelocityY(double v) { this.velocityY = v; }
    public double getVelocityZ() { return velocityZ; }
    public void setVelocityZ(double v) { this.velocityZ = v; }
    public void setVelocity(double x, double y, double z) { this.velocityX = x; this.velocityY = y; this.velocityZ = z; }
    public double getServerVelocityX() { return serverVelocityX; }
    public void setServerVelocityX(double v) { this.serverVelocityX = v; }
    public double getServerVelocityY() { return serverVelocityY; }
    public void setServerVelocityY(double v) { this.serverVelocityY = v; }
    public double getServerVelocityZ() { return serverVelocityZ; }
    public void setServerVelocityZ(double v) { this.serverVelocityZ = v; }
    public boolean isVelocityReceived() { return velocityReceived; }
    public void setVelocityReceived(boolean v) { this.velocityReceived = v; }
    public boolean isAllowFlight() { return allowFlight; }
    public void setAllowFlight(boolean f) { this.allowFlight = f; }
    public double getTeleportX() { return teleportX; }
    public double getTeleportY() { return teleportY; }
    public double getTeleportZ() { return teleportZ; }
    public void setTeleportPosition(double x, double y, double z) { this.teleportX = x; this.teleportY = y; this.teleportZ = z; }
    public ConcurrentHashMap<String, Integer> getViolationLevels() { return violationLevels; }
    public ConcurrentHashMap<String, Double> getBuffers() { return buffers; }
    public int getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(int c) { this.attackCooldown = c; }
    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long t) { this.lastAttackTime = t; }
    public long getJoinTime() { return joinTime; }
    public float getYaw() { return rotation.yaw; }
    public void setYaw(float yaw) {
        RotationState old = this.rotation;
        this.rotation = new RotationState(yaw, old.pitch, old.yaw, old.lastPitch);
    }
    public float getPitch() { return rotation.pitch; }
    public void setPitch(float pitch) {
        RotationState old = this.rotation;
        this.rotation = new RotationState(old.yaw, pitch, old.lastYaw, old.lastPitch);
    }
    public float getLastYaw() { return rotation.lastYaw; }
    public float getLastPitch() { return rotation.lastPitch; }
    public boolean isMovedSinceTick() { return movedSinceTick; }
    public void setMovedSinceTick(boolean m) { this.movedSinceTick = m; }
    public boolean isValid() { return valid; }
    public void setValid(boolean v) { this.valid = v; }
    public boolean isAlertsEnabled() { return alertsEnabled; }
    public void setAlertsEnabled(boolean e) { this.alertsEnabled = e; }
    public ActionData getActionData() { return actionData; }
    public io.windfall.anticheat.core.compensation.CompensatedWorld getCompensatedWorld() { return compensatedWorld; }
    public void setCompensatedWorld(io.windfall.anticheat.core.compensation.CompensatedWorld w) { this.compensatedWorld = w; }
    public boolean isRespawned() { return respawned; }
    public void setRespawned(boolean r) { this.respawned = r; }

    public int getTotalViolationLevel() {
        int total = 0;
        for (int v : violationLevels.values()) total += v;
        return total;
    }

    public void updateCachedState() {
        try {
            ServerPlayerEntity sp = serverPlayer;
            net.minecraft.util.math.BlockPos blockPos = sp.getBlockPos();
            net.minecraft.world.World world = sp.getWorld();
            net.minecraft.block.BlockState blockState = world.getBlockState(blockPos);
            net.minecraft.block.Material material = blockState.getMaterial();
            this.cachedInWater = material.isLiquid();
            this.cachedInLava = world.getBlockState(blockPos).getBlock() == net.minecraft.block.Blocks.LAVA;
            this.cachedOnHoney = false;
        } catch (Exception e) {
            this.cachedInWater = false; this.cachedInLava = false; this.cachedOnHoney = false;
        }
        try {
            StatusEffectInstance speed = serverPlayer.getStatusEffect(StatusEffects.SPEED);
            StatusEffectInstance slowness = serverPlayer.getStatusEffect(StatusEffects.SLOWNESS);
            this.cachedSpeedMultiplier = speed != null ? 1.0 + (0.20 * Math.min(speed.getAmplifier() + 1, 5)) : 1.0;
            this.cachedSlownessMultiplier = slowness != null ? 1.0 + (-0.15 * Math.min(slowness.getAmplifier() + 1, 4)) : 1.0;
            this.cachedHasSlowFalling = serverPlayer.hasStatusEffect(StatusEffects.SLOW_FALLING);
            this.cachedHasLevitation = serverPlayer.hasStatusEffect(StatusEffects.LEVITATION);
            StatusEffectInstance levitation = serverPlayer.getStatusEffect(StatusEffects.LEVITATION);
            this.cachedLevitationAmplifier = levitation != null ? levitation.getAmplifier() + 1 : 1.0;
            this.cachedIsFallFlying = serverPlayer.getAbilities().flying || serverPlayer.isFallFlying();
            this.cachedHasRiptide = false;
        } catch (Exception e) {
            this.cachedSpeedMultiplier = 1.0; this.cachedSlownessMultiplier = 1.0;
            this.cachedHasSlowFalling = false; this.cachedHasLevitation = false;
            this.cachedLevitationAmplifier = 1.0; this.cachedIsFallFlying = false;
            this.cachedHasRiptide = false;
        }
    }

    public boolean isCachedInWater() { return cachedInWater; }
    public boolean isCachedInLava() { return cachedInLava; }
    public boolean isCachedOnHoney() { return cachedOnHoney; }
    public double getCachedSpeedMultiplier() { return cachedSpeedMultiplier; }
    public double getCachedSlownessMultiplier() { return cachedSlownessMultiplier; }
    public boolean isCachedHasSlowFalling() { return cachedHasSlowFalling; }
    public boolean isCachedHasLevitation() { return cachedHasLevitation; }
    public double getCachedLevitationAmplifier() { return cachedLevitationAmplifier; }
    public boolean isCachedIsFallFlying() { return cachedIsFallFlying; }
    public boolean isCachedHasRiptide() { return cachedHasRiptide; }
}
