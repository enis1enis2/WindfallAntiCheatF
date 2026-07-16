package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Macro A", stableKey="windfall.combat.macro", decay=0.02, setbackVl=20)
public class MacroCheck extends Check implements PacketCheck {

    private static final int MIN_PATTERN_REPEATS = 50;
    private static final int SLIDING_WINDOW_SIZE = 20;
    private static final int MIN_WINDOW_ENTRIES = 10;
    private static final long SNAPSHOT_WINDOW_MS = 5000;

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private static class PlayerState {
        final Deque<String> movementPatterns = new ArrayDeque<>();
        final Map<String, Integer> patternFrequency = new HashMap<>();
        final Deque<MovementSnapshot> recentSnapshots = new ArrayDeque<>();
        int totalSnapshots;
    }

    private static class MovementSnapshot {
        final boolean forward, backward, left, right, sprint;
        final long timestamp;

        MovementSnapshot(boolean forward, boolean backward, boolean left, boolean right, boolean sprint, long timestamp) {
            this.forward = forward;
            this.backward = backward;
            this.left = left;
            this.right = right;
            this.sprint = sprint;
            this.timestamp = timestamp;
        }

        String toKey() {
            StringBuilder sb = new StringBuilder();
            if (forward) sb.append('W');
            if (backward) sb.append('S');
            if (left) sb.append('A');
            if (right) sb.append('D');
            if (sprint) sb.append('_');
            if (sb.length() == 0) sb.append("IDLE");
            return sb.toString();
        }
    }

    private PlayerState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)) return;

        PlayerState state = getState(player.getUuid());
        long now = System.currentTimeMillis();

        double dx = player.getX() - player.getLastX();
        double dz = player.getZ() - player.getLastZ();

        boolean forward = Math.abs(dz) > 0.05 && dz < 0;
        boolean backward = Math.abs(dz) > 0.05 && dz > 0;
        boolean left = Math.abs(dx) > 0.05 && dx < 0;
        boolean right = Math.abs(dx) > 0.05 && dx > 0;
        boolean sprint = player.isSprinting();

        MovementSnapshot snap = new MovementSnapshot(forward, backward, left, right, sprint, now);
        state.recentSnapshots.addLast(snap);
        while (!state.recentSnapshots.isEmpty() && now - state.recentSnapshots.peekFirst().timestamp > SNAPSHOT_WINDOW_MS) {
            MovementSnapshot old = state.recentSnapshots.pollFirst();
            String oldKey = old.toKey();
            state.patternFrequency.merge(oldKey, -1, Integer::sum);
            if (state.patternFrequency.getOrDefault(oldKey, 0) <= 0) state.patternFrequency.remove(oldKey);
        }

        String currentKey = snap.toKey();
        state.movementPatterns.addLast(currentKey);
        while (state.movementPatterns.size() > SLIDING_WINDOW_SIZE) {
            String removed = state.movementPatterns.pollFirst();
            state.patternFrequency.merge(removed, -1, Integer::sum);
            if (state.patternFrequency.getOrDefault(removed, 0) <= 0) state.patternFrequency.remove(removed);
        }
        state.patternFrequency.merge(currentKey, 1, Integer::sum);
        state.totalSnapshots++;

        int count = state.patternFrequency.getOrDefault(currentKey, 0);

        if (state.totalSnapshots >= MIN_WINDOW_ENTRIES && count >= MIN_PATTERN_REPEATS) {
            double ratio = (double) count / state.totalSnapshots;
            if (ratio > 0.8) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 3.0) { flag(player); resetBuffer(player); }
            }
        } else {
            decreaseBuffer(player, 0.05);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {}

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }
}
