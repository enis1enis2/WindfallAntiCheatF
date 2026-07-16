package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Motion A", stableKey="windfall.movement.motion", decay=0.02, setbackVl=20)
public class MotionCheck extends Check implements PacketCheck {

    private static final double ZERO_HORIZONTAL_THRESHOLD = 0.0001;
    private static final double CONSTANT_UPWARD_THRESHOLD = 0.01;
    private static final int UPWARD_VELOCITY_TICK_THRESHOLD = 5;
    private static final double ACCELERATION_CONSISTENCY_THRESHOLD = 0.00005;
    private static final int ACCELERATION_WINDOW = 10;
    private static final double DIRECTION_MISMATCH_THRESHOLD = 0.5;

    private final Map<UUID, MotionState> playerStates = new ConcurrentHashMap<>();

    private static final class MotionState {
        int constantUpwardTicks;
        double lastDeltaY;

        double[] recentAccels = new double[ACCELERATION_WINDOW];
        int accelIndex;
        double lastSpeed;
        boolean speedInitialized;

        double lastVelocityX, lastVelocityZ;
        boolean velocityInitialized;
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof PlayerMoveC2SPacket)) return;

        MotionState state = playerStates.computeIfAbsent(player.getUuid(), k -> new MotionState());

        double deltaX = player.getDeltaX();
        double deltaY = player.getDeltaY();
        double deltaZ = player.getDeltaZ();

        detectConstantUpward(state, player, deltaY);
        detectZeroHorizontalAirborne(state, player, deltaX, deltaZ);
        detectAccelerationConsistency(state, player);
        detectDirectionMismatch(state, player);
    }

    private void detectConstantUpward(MotionState state, WindfallPlayer player, double deltaY) {
        if (!player.isOnGround() && deltaY > CONSTANT_UPWARD_THRESHOLD
                && Math.abs(deltaY - state.lastDeltaY) < 0.001) {
            state.constantUpwardTicks++;
        } else {
            state.constantUpwardTicks = 0;
        }

        if (state.constantUpwardTicks >= UPWARD_VELOCITY_TICK_THRESHOLD) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        state.lastDeltaY = deltaY;
    }

    private void detectZeroHorizontalAirborne(MotionState state, WindfallPlayer player,
                                               double deltaX, double deltaZ) {
        if (player.isOnGround()) return;
        if (player.isFlying() || player.isGliding() || player.isSwimming() || player.isClimbing()) return;

        double horizSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double vertSpeed = Math.abs(player.getDeltaY());

        if (vertSpeed > 0.1 && horizSpeed < ZERO_HORIZONTAL_THRESHOLD) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (vertSpeed < 0.01 && horizSpeed < ZERO_HORIZONTAL_THRESHOLD && !player.isOnGround()) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    private void detectAccelerationConsistency(MotionState state, WindfallPlayer player) {
        double speed = player.getHorizontalSpeed();

        if (!state.speedInitialized) {
            state.lastSpeed = speed;
            state.speedInitialized = true;
            return;
        }

        double accel = speed - state.lastSpeed;
        state.recentAccels[state.accelIndex % ACCELERATION_WINDOW] = accel;
        state.accelIndex++;

        if (state.accelIndex >= ACCELERATION_WINDOW) {
            double sum = 0;
            int count = 0;
            double sumSq = 0;
            for (double a : state.recentAccels) {
                sum += a;
                sumSq += a * a;
                count++;
            }
            double mean = sum / count;
            double variance = (sumSq / count) - (mean * mean);

            if (variance < ACCELERATION_CONSISTENCY_THRESHOLD && speed > 0.05) {
                increaseBuffer(player, 0.3);
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        state.lastSpeed = speed;
    }

    private void detectDirectionMismatch(MotionState state, WindfallPlayer player) {
        double serverVelX = player.getServerVelocityX();
        double serverVelZ = player.getServerVelocityZ();
        double actualDeltaX = player.getDeltaX();
        double actualDeltaZ = player.getDeltaZ();

        double serverSpeed = Math.sqrt(serverVelX * serverVelX + serverVelZ * serverVelZ);
        if (serverSpeed < 0.01) return;

        double actualSpeed = Math.sqrt(actualDeltaX * actualDeltaX + actualDeltaZ * actualDeltaZ);
        if (actualSpeed < 0.01) return;

        double dotProduct = (serverVelX * actualDeltaX + serverVelZ * actualDeltaZ)
                / (serverSpeed * actualSpeed);
        double angleDiff = Math.acos(Math.max(-1, Math.min(1, dotProduct)));

        if (angleDiff > DIRECTION_MISMATCH_THRESHOLD) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(UUID uuid) {
        playerStates.remove(uuid);
    }
}
