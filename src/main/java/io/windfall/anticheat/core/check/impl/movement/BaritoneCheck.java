package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Baritone A", stableKey="windfall.movement.baritone", decay=0.02, setbackVl=20)
public class BaritoneCheck extends Check implements PacketCheck {

    private static final int STRAIGHT_YAW_TICK_THRESHOLD = 40;
    private static final int CONSISTENT_SPEED_TICK_THRESHOLD = 20;
    private static final double YAW_PRECISION = 0.001;
    private static final double SPEED_PRECISION = 0.0001;
    private static final double MIN_SPEED = 0.01;
    private static final double CIRCULAR_ANGLE_STEP_TOLERANCE = 0.05;

    private final Map<UUID, BaritoneState> playerStates = new ConcurrentHashMap<>();

    private static final class BaritoneState {
        int straightYawTicks;
        float lastYaw;
        boolean yawInitialized;

        double[] recentSpeeds = new double[CONSISTENT_SPEED_TICK_THRESHOLD];
        int speedIndex;

        double[] recentAngles = new double[30];
        int angleIndex;
        double lastAngle;
        boolean angleInitialized;
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof PlayerMoveC2SPacket)) return;

        PlayerMoveC2SPacket move = (PlayerMoveC2SPacket) packet;
        if (!move.changesLook()) return;

        BaritoneState state = playerStates.computeIfAbsent(player.getUuid(), k -> new BaritoneState());
        float yaw = player.getYaw();
        double speed = player.getHorizontalSpeed();

        detectStraightLine(state, player, yaw);
        detectConsistentSpeed(state, player, speed);
        detectCircularPattern(state, player, yaw);
    }

    private void detectStraightLine(BaritoneState state, WindfallPlayer player, float yaw) {
        if (!state.yawInitialized) {
            state.lastYaw = yaw;
            state.yawInitialized = true;
            state.straightYawTicks = 0;
            return;
        }

        float yawDiff = Math.abs(yaw - state.lastYaw);
        if (yawDiff < YAW_PRECISION || Math.abs(yawDiff - 360.0f) < YAW_PRECISION) {
            state.straightYawTicks++;
        } else {
            state.straightYawTicks = 0;
        }

        if (state.straightYawTicks >= STRAIGHT_YAW_TICK_THRESHOLD) {
            double speed = player.getHorizontalSpeed();
            if (speed > MIN_SPEED) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        state.lastYaw = yaw;
    }

    private void detectConsistentSpeed(BaritoneState state, WindfallPlayer player, double speed) {
        if (speed < MIN_SPEED) {
            state.recentSpeeds[state.speedIndex % CONSISTENT_SPEED_TICK_THRESHOLD] = 0;
            state.speedIndex++;
            return;
        }

        state.recentSpeeds[state.speedIndex % CONSISTENT_SPEED_TICK_THRESHOLD] = speed;
        state.speedIndex++;

        if (state.speedIndex >= CONSISTENT_SPEED_TICK_THRESHOLD) {
            double sum = 0;
            double sumSq = 0;
            int count = 0;
            for (double s : state.recentSpeeds) {
                if (s > 0) {
                    sum += s;
                    sumSq += s * s;
                    count++;
                }
            }
            if (count < CONSISTENT_SPEED_TICK_THRESHOLD / 2) return;

            double mean = sum / count;
            double variance = (sumSq / count) - (mean * mean);

            if (variance < SPEED_PRECISION && count >= CONSISTENT_SPEED_TICK_THRESHOLD - 2) {
                increaseBuffer(player, 0.8);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }
    }

    private void detectCircularPattern(BaritoneState state, WindfallPlayer player, float yaw) {
        double deltaYaw = yaw - state.lastYaw;
        if (deltaYaw > 180) deltaYaw -= 360;
        if (deltaYaw < -180) deltaYaw += 360;

        state.recentAngles[state.angleIndex % 30] = deltaYaw;
        state.angleIndex++;

        if (state.angleIndex >= 30 && deltaYaw != 0) {
            boolean allSame = true;
            double firstNonZero = 0;
            for (double a : state.recentAngles) {
                if (Math.abs(a) < 0.001) continue;
                if (firstNonZero == 0) {
                    firstNonZero = a;
                } else if (Math.abs(a - firstNonZero) > CIRCULAR_ANGLE_STEP_TOLERANCE) {
                    allSame = false;
                    break;
                }
            }
            if (allSame && firstNonZero != 0 && player.getHorizontalSpeed() > MIN_SPEED) {
                increaseBuffer(player, 0.4);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        state.lastYaw = yaw;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(UUID uuid) {
        playerStates.remove(uuid);
    }
}
