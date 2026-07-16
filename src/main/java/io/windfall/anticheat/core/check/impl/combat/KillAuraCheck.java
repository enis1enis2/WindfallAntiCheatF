package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="KillAura A", stableKey="windfall.combat.killaura", decay=0.02, setbackVl=20)
public class KillAuraCheck extends Check implements PacketCheck {

    private static final int MAX_UNIQUE_TARGETS_PER_SECOND = 4;
    private static final double SNAP_SYMMETRY_THRESHOLD = 0.95;
    private static final int MIN_SNAPS_FOR_SYMMETRY = 3;
    private static final double SNAP_DELTA_THRESHOLD = 10.0;
    private static final int MAX_YAW_SAMPLES = 50;

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private static class PlayerState {
        final Deque<TargetRecord> recentTargets = new ArrayDeque<>();
        final Deque<Float> yawSamples = new ArrayDeque<>();
        int snapCount;
        int symmetryMatches;
        int totalSamples;

        static class TargetRecord {
            final int entityId;
            final long timestamp;
            TargetRecord(int entityId, long timestamp) {
                this.entityId = entityId;
                this.timestamp = timestamp;
            }
        }
    }

    private PlayerState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;

        final boolean[] isAttack = {false};
        p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
            public void interact(net.minecraft.util.Hand hand) {}
            public void attack() { isAttack[0] = true; }
            public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {}
        });
        if (!isAttack[0]) return;

        int targetId;
        try {
            net.minecraft.entity.Entity target = p.getEntity(player.getServerPlayer().getServerWorld());
            targetId = target != null ? target.getId() : -1;
        } catch (Exception e) {
            targetId = -1;
        }
        long now = System.currentTimeMillis();
        PlayerState state = getState(player.getUuid());

        state.recentTargets.addLast(new PlayerState.TargetRecord(targetId, now));
        while (!state.recentTargets.isEmpty() && now - state.recentTargets.peekFirst().timestamp > 1000) {
            state.recentTargets.pollFirst();
        }

        Set<Integer> uniqueTargets = new HashSet<>();
        for (PlayerState.TargetRecord record : state.recentTargets) {
            uniqueTargets.add(record.entityId);
        }
        if (uniqueTargets.size() > MAX_UNIQUE_TARGETS_PER_SECOND) {
            increaseBuffer(player, (uniqueTargets.size() - MAX_UNIQUE_TARGETS_PER_SECOND) * 0.5);
            if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
        } else {
            decreaseBuffer(player, 0.05);
        }

        float yaw = player.getYaw();
        float lastYaw = player.getLastYaw();
        float deltaYaw = yaw - lastYaw;
        state.yawSamples.addLast(deltaYaw);
        while (state.yawSamples.size() > MAX_YAW_SAMPLES) state.yawSamples.pollFirst();
        state.totalSamples++;

        if (Math.abs(deltaYaw) > SNAP_DELTA_THRESHOLD) {
            state.snapCount++;
            float normalizedDelta = io.windfall.anticheat.core.util.MathUtil.normalizeYaw(deltaYaw);
            if (state.yawSamples.size() >= 2) {
                Iterator<Float> it = state.yawSamples.iterator();
                float prevDelta = 0;
                int idx = 0;
                int size = state.yawSamples.size();
                while (it.hasNext()) {
                    float d = it.next();
                    if (idx == size - 2) { prevDelta = d; break; }
                    idx++;
                }
                float prevNorm = io.windfall.anticheat.core.util.MathUtil.normalizeYaw(prevDelta);
                if (Math.abs(Math.abs(normalizedDelta) - Math.abs(prevNorm)) < 2.0
                        && Math.signum(normalizedDelta) == Math.signum(prevNorm)) {
                    state.symmetryMatches++;
                }
            }
        }

        if (state.totalSamples >= MIN_SNAPS_FOR_SYMMETRY && state.snapCount >= MIN_SNAPS_FOR_SYMMETRY) {
            double ratio = (double) state.symmetryMatches / state.totalSamples;
            if (ratio > SNAP_SYMMETRY_THRESHOLD) {
                increaseBuffer(player, (ratio - SNAP_SYMMETRY_THRESHOLD) * 5.0);
                if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
                state.snapCount = 0;
                state.symmetryMatches = 0;
                state.totalSamples = 0;
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {}

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }
}
