package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Flight A", stableKey="windfall.movement.fly", decay=0.01, setbackVl=15, compat={CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier=1.3)
public class FlightCheck extends Check implements PacketCheck {

    private static final double JUMP_MOMENTUM = 0.42;
    private static final double VERTICAL_TOLERANCE = 0.05;
    private static final int HOVER_TICK_THRESHOLD = 20;
    private static final double HOVER_DELTA_THRESHOLD = 0.005;
    private static final double NO_FALL_DISTANCE = 3.0;
    private static final double NO_FALL_VELOCITY_THRESHOLD = 0.5;
    private static final double MAX_UPWARD_DEVIATION = 0.5;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        double expectedDeltaY;
        double fallDistance;
        int hoverTicks;
        int airborneTicks;
        boolean wasOnGround;
        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;
        if (player.isAllowFlight()) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

        double deltaY = player.getDeltaY();
        boolean currentOnGround = player.isOnGround();

        if (currentOnGround) {
            state.expectedDeltaY = 0;
            state.fallDistance = 0;
            state.airborneTicks = 0;
            state.hoverTicks = 0;
            state.wasOnGround = true;
            return;
        }

        state.airborneTicks++;

        if (deltaY < -0.001) {
            state.fallDistance += Math.abs(deltaY);
        } else {
            state.fallDistance = 0;
        }

        if (state.wasOnGround && !currentOnGround) {
            if (deltaY >= JUMP_MOMENTUM - 0.01 && deltaY <= JUMP_MOMENTUM + 0.15) {
                state.expectedDeltaY = JUMP_MOMENTUM;
            } else if (Math.abs(deltaY) < 0.01) {
                state.expectedDeltaY = 0;
            }
        }

        double predictedDeltaY = predictAirDeltaY(state.expectedDeltaY);

        double verticalDelta = deltaY - predictedDeltaY;

        boolean isHovering = Math.abs(deltaY) < HOVER_DELTA_THRESHOLD
                          && Math.abs(state.expectedDeltaY) < HOVER_DELTA_THRESHOLD;

        if (isHovering && state.airborneTicks > 3) {
            state.hoverTicks++;
            if (state.hoverTicks > HOVER_TICK_THRESHOLD) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                    state.hoverTicks = 0;
                }
            }
        } else {
            state.hoverTicks = Math.max(0, state.hoverTicks - 1);
        }

        if (Math.abs(deltaY) > 0.01 && state.airborneTicks > 3) {
            double deviationRatio = Math.abs(verticalDelta) / Math.max(Math.abs(predictedDeltaY), 0.001);

            if (deltaY > 0 && state.expectedDeltaY <= 0) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else if (deviationRatio > 2.0) {
                flag(player);
                resetBuffer(player);
            } else if (deviationRatio > 0.5) {
                increaseBuffer(player, 0.3 * Math.min(deviationRatio, 2.0));
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }

        if (player.isOnGround() && !player.isLastOnGround() && state.fallDistance > NO_FALL_DISTANCE) {
            if (deltaY < -NO_FALL_VELOCITY_THRESHOLD) {
                increaseBuffer(player, 0.8);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        if (!currentOnGround && deltaY > 0 && state.airborneTicks > 5) {
            ServerWorld world = (ServerWorld) player.getServerPlayer().getWorld();
            BlockPos below = new BlockPos((int) Math.floor(player.getX()), (int) Math.floor(player.getY()) - 1, (int) Math.floor(player.getZ()));
            boolean hasSolidBelow = world.getBlockState(below).isSolidBlock(world, below);
            if (!hasSolidBelow && deltaY > MAX_UPWARD_DEVIATION) {
                increaseBuffer(player, deltaY / MAX_UPWARD_DEVIATION);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        state.expectedDeltaY = deltaY;
        state.wasOnGround = currentOnGround;
    }

    private double predictAirDeltaY(double lastDeltaY) {
        return (lastDeltaY - 0.08) * 0.98;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }
}
