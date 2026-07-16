package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Phase A", stableKey="windfall.movement.phase", decay=0.02, setbackVl=20)
public class PhaseCheck extends Check implements PacketCheck {

    private static final double STEP_HEIGHT = 0.6;
    private static final double STEP_TOLERANCE = 0.3;
    private static final int RAYTRACE_STEPS = 10;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        double lastX, lastY, lastZ;
        boolean lastOnGround;
        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double lastX = state.lastX;
        double lastY = state.lastY;
        double lastZ = state.lastZ;

        if (lastX == 0 && lastY == 0 && lastZ == 0) {
            state.lastX = x;
            state.lastY = y;
            state.lastZ = lastZ;
            return;
        }

        double dx = x - lastX;
        double dy = y - lastY;
        double dz = z - lastZ;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double verticalDist = Math.abs(dy);

        double maxStep = STEP_HEIGHT + STEP_TOLERANCE;
        if (player.isSneaking()) {
            maxStep = 0.6;
        }

        if (dy > 0 && dy > maxStep && horizontalDist < 0.5) {
            ServerWorld world = (ServerWorld) player.getServerPlayer().getWorld();
            boolean blockedAbove = checkPathBlocked(world, lastX, lastY, lastZ, x, y, z);
            if (blockedAbove) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        if (horizontalDist > 1.0) {
            ServerWorld world = (ServerWorld) player.getServerPlayer().getWorld();
            boolean pathBlocked = checkPathBlocked(world, lastX, lastY, lastZ, x, y, z);
            if (pathBlocked) {
                increaseBuffer(player, 0.8);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            decreaseBuffer(player, 0.1);
        }

        state.lastX = x;
        state.lastY = y;
        state.lastZ = z;
        state.lastOnGround = player.isOnGround();
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }

    private boolean checkPathBlocked(ServerWorld world, double x1, double y1, double z1, double x2, double y2, double z2) {
        Vec3d start = new Vec3d(x1, y1 + 0.01, z1);
        Vec3d end = new Vec3d(x2, y2 + 0.01, z2);
        Vec3d dir = end.subtract(start);

        double dist = dir.length();
        if (dist < 0.01) return false;

        Vec3d step = dir.normalize().multiply(dist / RAYTRACE_STEPS);

        for (int i = 0; i <= RAYTRACE_STEPS; i++) {
            Vec3d point = start.add(step.multiply(i));
            BlockPos blockPos = new BlockPos((int) Math.floor(point.x), (int) Math.floor(point.y), (int) Math.floor(point.z));
            BlockState state = world.getBlockState(blockPos);

            if (state.isOf(Blocks.AIR) || state.isOf(Blocks.CAVE_AIR) || state.isOf(Blocks.VOID_AIR)
                || state.getFluidState().isOf(net.minecraft.fluid.Fluids.WATER)
                || state.getFluidState().isOf(net.minecraft.fluid.Fluids.LAVA)
                || state.isOf(Blocks.WATER) || state.isOf(Blocks.LAVA)
                || state.isOf(Blocks.KELP) || state.isOf(Blocks.KELP_PLANT)
                || state.isOf(Blocks.SEAGRASS) || state.isOf(Blocks.TALL_SEAGRASS)
                || state.isOf(Blocks.SEA_PICKLE)) {
                continue;
            }

            if (state.isSolidBlock(world, blockPos)) {
                return true;
            }
        }

        return false;
    }
}
