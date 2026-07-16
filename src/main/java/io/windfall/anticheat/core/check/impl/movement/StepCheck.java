package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Step A", stableKey="windfall.movement.step", decay=0.02, setbackVl=20)
public class StepCheck extends Check implements PacketCheck {

    private static final double DEFAULT_STEP_HEIGHT = 0.6;
    private static final double MAX_STEP_HEIGHT = 2.0;
    private static final double STEP_TOLERANCE = 0.1;
    private static final double MIN_HORIZONTAL_FOR_STEP = 0.05;

    private final ConcurrentHashMap<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        int consecutiveViolations;
        int stepUpTicks;
        double lastY;
    }

    private PlayerState getState(UUID uuid) {
        return playerStates.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;

        PlayerState state = getState(player.getUuid());

        double deltaY = player.getY() - player.getLastY();
        double absDeltaY = Math.abs(deltaY);
        double horizontalDist = Math.sqrt(
            (player.getX() - player.getLastX()) * (player.getX() - player.getLastX()) +
            (player.getZ() - player.getLastZ()) * (player.getZ() - player.getLastZ())
        );

        double maxStep = DEFAULT_STEP_HEIGHT;
        if (player.isSwimming()) {
            maxStep = MAX_STEP_HEIGHT;
        }
        if (player.isClimbing()) {
            maxStep = 3.5;
        }

        maxStep += STEP_TOLERANCE;

        if (deltaY > 0 && player.isOnGround() && absDeltaY > maxStep) {
            try {
                ServerWorld world = (ServerWorld) player.getServerPlayer().getWorld();
                BlockPos feetPos = new BlockPos((int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()));
                boolean onHoney = world.getBlockState(feetPos.down()).isOf(Blocks.HONEY_BLOCK);
                boolean onSlime = world.getBlockState(feetPos.down()).isOf(Blocks.SLIME_BLOCK);

                if (onHoney || onSlime) {
                    maxStep = 1.2;
                }
            } catch (Exception e) {
                // World access failed, use default maxStep
            }

            if (absDeltaY > maxStep) {
                if (horizontalDist < MIN_HORIZONTAL_FOR_STEP && deltaY > 0) {
                    state.consecutiveViolations++;
                    state.stepUpTicks = 5;
                } else if (horizontalDist >= MIN_HORIZONTAL_FOR_STEP) {
                    state.stepUpTicks--;
                    if (state.stepUpTicks <= 0) {
                        state.consecutiveViolations = Math.max(0, state.consecutiveViolations - 1);
                    }
                }

                double severity = absDeltaY - maxStep;
                double bufferInc = severity * (1.0 + state.consecutiveViolations * 0.5);
                increaseBuffer(player, bufferInc);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            if (state.stepUpTicks > 0) state.stepUpTicks--;
            else state.consecutiveViolations = Math.max(0, state.consecutiveViolations - 1);
            decreaseBuffer(player, 0.1);
        }

        state.lastY = player.getY();
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(UUID uuid) {
        playerStates.remove(uuid);
    }
}
