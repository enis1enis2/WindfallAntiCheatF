package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.util.MathUtil;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Aim A", stableKey="windfall.combat.aim", decay=0.01, setbackVl=15, relaxMultiplier=0.7)
public class AimCheck extends Check implements PacketCheck {

    private static final double MIN_DELTA_THRESHOLD = 0.015;
    private static final double INSTANT_SNAP_THRESHOLD = 90.0;
    private static final int SNAPSHOT_WINDOW = 20;
    private static final double ZERO_CHANGE_THRESHOLD = 0.001;
    private static final double MIN_VARIANCE_RATIO = 0.05;

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private static class PlayerState {
        float lastYaw, lastPitch;
        final Deque<Double> angleDeltas = new ArrayDeque<>();
        final Deque<Double> yawDeltas = new ArrayDeque<>();
        final Deque<Double> pitchDeltas = new ArrayDeque<>();
        int snapCount;
        int zeroChangeCount;
        int totalMoves;
    }

    private PlayerState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)) return;

        PlayerState state = getState(player.getUuid());
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        state.totalMoves++;

        float deltaYaw = yaw - state.lastYaw;
        float deltaPitch = pitch - state.lastPitch;
        float absDeltaYaw = Math.abs(deltaYaw);
        float absDeltaPitch = Math.abs(deltaPitch);

        if (state.lastYaw != 0 || state.lastPitch != 0) {
            if (absDeltaYaw < ZERO_CHANGE_THRESHOLD && absDeltaPitch < ZERO_CHANGE_THRESHOLD) {
                state.zeroChangeCount++;
            }
        }

        state.lastYaw = yaw;
        state.lastPitch = pitch;

        if (Math.abs(player.getDeltaX()) < 0.001 && Math.abs(player.getDeltaZ()) < 0.001 && Math.abs(player.getDeltaY()) < 0.001) {
            return;
        }

        double deltaAngle = MathUtil.getAngleDelta(yaw, pitch, yaw - deltaYaw, pitch - deltaPitch);
        if (deltaAngle < MIN_DELTA_THRESHOLD) return;

        state.angleDeltas.addLast(deltaAngle);
        while (state.angleDeltas.size() > SNAPSHOT_WINDOW) state.angleDeltas.pollFirst();

        state.yawDeltas.addLast((double) absDeltaYaw);
        while (state.yawDeltas.size() > SNAPSHOT_WINDOW) state.yawDeltas.pollFirst();
        state.pitchDeltas.addLast((double) absDeltaPitch);
        while (state.pitchDeltas.size() > SNAPSHOT_WINDOW) state.pitchDeltas.pollFirst();

        if (deltaAngle > INSTANT_SNAP_THRESHOLD) {
            increaseBuffer(player, 1.5);
            if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
            return;
        }

        if (absDeltaYaw > 40) {
            state.snapCount++;
        }

        double sensitivity = player.getTransactionPing() / 50.0;
        double maxAngle = 40.0 + (sensitivity * 5.0);
        flagIfAboveThreshold(player, deltaAngle, maxAngle);

        if (state.yawDeltas.size() >= SNAPSHOT_WINDOW) {
            double yawMean = computeMean(state.yawDeltas);
            double yawStdDev = computeStdDev(state.yawDeltas, yawMean);
            if (yawMean > 0.5 && yawStdDev / yawMean < MIN_VARIANCE_RATIO) {
                increaseBuffer(player, 0.6);
                if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
                return;
            }

            double pitchMean = computeMean(state.pitchDeltas);
            double pitchStdDev = computeStdDev(state.pitchDeltas, pitchMean);
            if (pitchMean > 0.5 && pitchStdDev / pitchMean < MIN_VARIANCE_RATIO) {
                increaseBuffer(player, 0.6);
                if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
                return;
            }
        }

        if (state.snapCount >= 3 && state.angleDeltas.size() >= SNAPSHOT_WINDOW) {
            double consistency = 1.0 - (computeStdDev(state.angleDeltas, computeMean(state.angleDeltas)) / computeMean(state.angleDeltas));
            if (consistency > 0.85) {
                increaseBuffer(player, 0.4);
                if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
            }
            state.snapCount = Math.max(0, state.snapCount - 2);
        }

        if (state.totalMoves >= 40) {
            double zeroRatio = (double) state.zeroChangeCount / state.totalMoves;
            if (zeroRatio > 0.3) {
                increaseBuffer(player, 0.2);
                if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
            }
        }
    }

    private double computeMean(Deque<Double> values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.size();
    }

    private double computeStdDev(Deque<Double> values, double mean) {
        double sumSq = 0;
        for (double v : values) sumSq += (v - mean) * (v - mean);
        return Math.sqrt(sumSq / values.size());
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {}

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }
}
