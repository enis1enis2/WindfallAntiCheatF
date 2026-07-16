package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@CheckData(name="Velocity A", stableKey="windfall.movement.velocity", decay=0.01, setbackVl=30,
    compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class VelocityCheck extends Check implements PacketCheck {

    private static final double MIN_VELOCITY_THRESHOLD = 0.005;
    private static final int MAX_PENDING_VELOCITIES = 5;

    private static final class PlayerState {
        final ConcurrentLinkedDeque<PendingVelocity> pendingVelocities = new ConcurrentLinkedDeque<>();
        boolean velocityActive;
        double expectedDeltaX;
        double expectedDeltaY;
        double expectedDeltaZ;
        int velocityAge;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
        if (!(packet instanceof EntityVelocityUpdateS2CPacket)) return;

        EntityVelocityUpdateS2CPacket velPacket = (EntityVelocityUpdateS2CPacket) packet;
        if (velPacket.getEntityId() != player.getServerPlayer().getId()) return;

        double velX = velPacket.getVelocityX();
        double velY = velPacket.getVelocityY();
        double velZ = velPacket.getVelocityZ();

        if (Math.abs(velX) < MIN_VELOCITY_THRESHOLD
                && Math.abs(velY) < MIN_VELOCITY_THRESHOLD
                && Math.abs(velZ) < MIN_VELOCITY_THRESHOLD) {
            return;
        }

        PlayerState state = getState(player);
        while (state.pendingVelocities.size() >= MAX_PENDING_VELOCITIES) {
            state.pendingVelocities.removeFirst();
        }
        state.pendingVelocities.addLast(new PendingVelocity(velX, velY, velZ, System.currentTimeMillis()));
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (!player.isVelocityReceived()) return;

        PlayerState state = getState(player);

        if (!state.velocityActive) {
            PendingVelocity pv;
            while ((pv = state.pendingVelocities.peekFirst()) != null) {
                long age = System.currentTimeMillis() - pv.receivedAt;
                long timeout = 500L + Math.max(0, player.getTransactionPing());
                if (age > timeout) {
                    state.pendingVelocities.pollFirst();
                    continue;
                }
                state.pendingVelocities.pollFirst();
                state.velocityActive = true;

                double[] postDrag = applyPostVelocityPhysics(pv.velX, pv.velY, pv.velZ, player);
                state.expectedDeltaX = postDrag[0];
                state.expectedDeltaY = postDrag[1];
                state.expectedDeltaZ = postDrag[2];
                state.velocityAge = 0;
                return;
            }
            player.setVelocityReceived(false);
            return;
        }

        if (!state.velocityActive) {
            player.setVelocityReceived(false);
            return;
        }

        state.velocityAge++;

        double actualDeltaX = player.getDeltaX();
        double actualDeltaZ = player.getDeltaZ();
        double actualDeltaY = player.getDeltaY();

        double expectedHorizontalDist = Math.sqrt(state.expectedDeltaX * state.expectedDeltaX + state.expectedDeltaZ * state.expectedDeltaZ);
        double actualHorizontalDist = Math.sqrt(actualDeltaX * actualDeltaX + actualDeltaZ * actualDeltaZ);

        if (expectedHorizontalDist < MIN_VELOCITY_THRESHOLD && Math.abs(state.expectedDeltaY) < MIN_VELOCITY_THRESHOLD) {
            state.velocityActive = false;
            resetBuffer(player);
            player.setVelocityReceived(false);
            return;
        }

        double horizontalRatio;
        if (expectedHorizontalDist > MIN_VELOCITY_THRESHOLD) {
            horizontalRatio = actualHorizontalDist / expectedHorizontalDist;
        } else {
            horizontalRatio = actualHorizontalDist < MIN_VELOCITY_THRESHOLD ? 1.0 : 0.0;
        }

        double verticalRatio;
        if (Math.abs(state.expectedDeltaY) > MIN_VELOCITY_THRESHOLD) {
            verticalRatio = Math.abs(actualDeltaY) / Math.abs(state.expectedDeltaY);
        } else {
            verticalRatio = Math.abs(actualDeltaY) < MIN_VELOCITY_THRESHOLD ? 1.0 : 0.0;
        }

        double combinedRatio = (horizontalRatio + verticalRatio) / 2.0;

        state.velocityActive = false;
        player.setVelocityReceived(false);

        if (state.velocityAge > 5) {
            resetBuffer(player);
            return;
        }

        double tolerance = 1.0;
        if (isNearWall(player)) {
            tolerance *= 1.3;
        }

        double adjustedThreshold05 = 0.5 / tolerance;
        double adjustedThreshold08 = 0.8 / tolerance;

        if (combinedRatio < adjustedThreshold05) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (combinedRatio < adjustedThreshold08) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.5);
        }
    }

    private double[] applyPostVelocityPhysics(double velX, double velY, double velZ, WindfallPlayer player) {
        double groundFriction = 0.91;
        double gravity = Math.abs(PhysicsConstants.GRAVITY);
        double airDrag = PhysicsConstants.AIR_DRAG;

        double newDeltaX = velX * groundFriction;
        double newDeltaZ = velZ * groundFriction;

        double newDeltaY;
        if (player.isSwimming()) {
            newDeltaY = velY * PhysicsConstants.WATER_DRAG - 0.02;
        } else if (player.isClimbing()) {
            newDeltaY = Math.max(velY, -0.15);
            newDeltaX *= 0.2;
            newDeltaZ *= 0.2;
        } else {
            newDeltaY = (velY - gravity) * airDrag;
        }

        if (!player.isOnGround()) {
            double airAcceleration = player.getHorizontalSpeed() * 0.026;
            double maxPostVelHorizontal = Math.sqrt(newDeltaX * newDeltaX + newDeltaZ * newDeltaZ);
            if (maxPostVelHorizontal > MIN_VELOCITY_THRESHOLD) {
                double accelContribution = Math.min(airAcceleration, maxPostVelHorizontal * 0.1);
                newDeltaX += (player.getDeltaX() > 0 ? 1 : -1) * accelContribution;
                newDeltaZ += (player.getDeltaZ() > 0 ? 1 : -1) * accelContribution;
            }
        }

        return new double[]{newDeltaX, newDeltaY, newDeltaZ};
    }

    private boolean isNearWall(WindfallPlayer player) {
        ServerPlayerEntity sp = player.getServerPlayer();
        if (sp == null) return false;
        ServerWorld world = (ServerWorld) sp.getWorld();
        int px = (int) Math.floor(player.getX());
        int py = (int) Math.floor(player.getY());
        int pz = (int) Math.floor(player.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (world.getBlockState(new BlockPos(px + dx, py - 1, pz + dz)).isSolidBlock(world, new BlockPos(px + dx, py - 1, pz + dz))) return true;
                if (world.getBlockState(new BlockPos(px + dx, py, pz + dz)).isSolidBlock(world, new BlockPos(px + dx, py, pz + dz))) return true;
                if (world.getBlockState(new BlockPos(px + dx, py + 1, pz + dz)).isSolidBlock(world, new BlockPos(px + dx, py + 1, pz + dz))) return true;
            }
        }
        return false;
    }

    private static final class PendingVelocity {
        final double velX, velY, velZ;
        final long receivedAt;

        PendingVelocity(double velX, double velY, double velZ, long receivedAt) {
            this.velX = velX;
            this.velY = velY;
            this.velZ = velZ;
            this.receivedAt = receivedAt;
        }
    }
}
