package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Speed A", stableKey="windfall.movement.speed", decay=0.01, setbackVl=20, compat={CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier=1.5)
public class SpeedCheck extends Check implements PacketCheck {

    private static final double WALK_SPEED = 0.2257;
    private static final double SPRINT_SPEED = 0.2873;
    private static final double SNEAK_SPEED = 0.1231;
    private static final double ICE_MULTIPLIER = 2.5;
    private static final double SLIME_MULTIPLIER = 1.4;
    private static final double SOUL_SAND_MULTIPLIER = 0.4;
    private static final double SPEED_TOLERANCE = 1.05;
    private static final double MIN_SPEED_FLAG_BUFFER = 3.0;
    private static final double PRE_1_18_2_THRESHOLD = 0.03;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        double lastX, lastY, lastZ;
        boolean wasOnGround;
        int airborneTicks;
        double maxObservedSpeed;
        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

        double horizontalSpeed = player.getHorizontalSpeed();
        if (horizontalSpeed < 0.005) {
            decreaseBuffer(player, 0.05);
            return;
        }

        if (horizontalSpeed > state.maxObservedSpeed) {
            state.maxObservedSpeed = horizontalSpeed;
        }

        ServerWorld world = (ServerWorld) player.getServerPlayer().getWorld();
        BlockPos blockPos = new BlockPos((int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()));

        boolean onIce = isOnIce(world, blockPos);
        boolean onSlime = isOnSlime(world, blockPos);
        boolean onSoulSand = isOnSoulSand(world, blockPos);
        boolean inCobweb = isInCobweb(world, blockPos);

        double maxSpeed;

        if (player.isOnGround()) {
            if (player.isSneaking()) {
                maxSpeed = SNEAK_SPEED;
            } else if (player.isSprinting()) {
                maxSpeed = SPRINT_SPEED;
            } else {
                maxSpeed = WALK_SPEED;
            }

            if (onIce) maxSpeed *= ICE_MULTIPLIER;
            if (onSlime) maxSpeed *= SLIME_MULTIPLIER;
            if (onSoulSand) maxSpeed *= SOUL_SAND_MULTIPLIER;
            if (inCobweb) maxSpeed *= 0.1;
        } else {
            double airSpeed = player.isSprinting() ? SPRINT_SPEED : WALK_SPEED;
            maxSpeed = airSpeed;
            state.airborneTicks++;
            if (state.airborneTicks > 3) {
                maxSpeed *= 1.5;
            }
        }

        double potionMultiplier = player.getCachedSpeedMultiplier() * player.getCachedSlownessMultiplier();
        maxSpeed *= Math.max(0.5, potionMultiplier);

        if (horizontalSpeed > maxSpeed * SPEED_TOLERANCE) {
            double exceedRatio = horizontalSpeed / Math.max(maxSpeed, 0.001);
            if (exceedRatio > 2.0) {
                flag(player);
                resetBuffer(player);
            } else {
                increaseBuffer(player, 0.5 * (exceedRatio - 1.0));
                if (getBuffer(player) > MIN_SPEED_FLAG_BUFFER) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            decreaseBuffer(player, 0.1);
        }

        state.lastX = player.getX();
        state.lastY = player.getY();
        state.lastZ = player.getZ();
        state.wasOnGround = player.isOnGround();

        if (player.isOnGround()) {
            state.airborneTicks = 0;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }

    private boolean isOnIce(ServerWorld world, BlockPos pos) {
        BlockPos below = pos.down();
        return world.getBlockState(below).isOf(Blocks.PACKED_ICE)
            || world.getBlockState(below).isOf(Blocks.BLUE_ICE)
            || world.getBlockState(below).isOf(Blocks.ICE);
    }

    private boolean isOnSlime(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos.down()).isOf(Blocks.SLIME_BLOCK);
    }

    private boolean isOnSoulSand(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos.down()).isOf(Blocks.SOUL_SAND)
            || world.getBlockState(pos.down()).isOf(Blocks.SOUL_SOIL);
    }

    private boolean isInCobweb(ServerWorld world, BlockPos pos) {
        return world.getBlockState(pos).isOf(Blocks.COBWEB)
            || world.getBlockState(pos.up()).isOf(Blocks.COBWEB);
    }
}
