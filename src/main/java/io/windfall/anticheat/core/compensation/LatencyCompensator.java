package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LatencyCompensator {
    private final Map<UUID, WorldChangeQueue> playerQueues = new ConcurrentHashMap<>();

    static class WorldChangeQueue {
        final java.util.Queue<WorldChange> changes = new java.util.concurrent.ConcurrentLinkedQueue<>();
    }

    public void addWorldChange(UUID uuid, WorldChange change) {
        playerQueues.computeIfAbsent(uuid, k -> new WorldChangeQueue()).changes.offer(change);
    }

    public void processDeferredChanges(UUID uuid, WindfallPlayer player) {
        WorldChangeQueue queue = playerQueues.get(uuid);
        if (queue == null) return;
        int ping = player.getTransactionPing();
        long now = System.currentTimeMillis();
        while (!queue.changes.isEmpty()) {
            WorldChange change = queue.changes.peek();
            if (now - change.getTimestamp() >= ping) {
                change.apply(player);
                queue.changes.poll();
            } else break;
        }
    }

    public void onPlayerQuit(UUID uuid) {
        playerQueues.remove(uuid);
    }

    public void pruneTickHistory(int currentTick) {}
}
