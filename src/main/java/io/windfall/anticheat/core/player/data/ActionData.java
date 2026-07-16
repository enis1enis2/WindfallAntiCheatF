package io.windfall.anticheat.core.player.data;

import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ActionData {
    private final WindfallPlayer player;
    private final ConcurrentLinkedQueue<BlockAction> recentBlockPlacements = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<BlockAction> recentBlockBreaks = new ConcurrentLinkedQueue<>();

    static class BlockAction {
        final int x, y, z;
        final long timestamp;
        BlockAction(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private volatile boolean usingItem;
    private volatile long useItemStart;

    public ActionData(WindfallPlayer player) { this.player = player; }

    public boolean isUsingItem() { return usingItem; }
    public void setUsingItem(boolean using) { this.usingItem = using; if (using) useItemStart = System.currentTimeMillis(); }
    public long getUseItemStart() { return useItemStart; }

    public void recordBlockPlace(int x, int y, int z) {
        recentBlockPlacements.offer(new BlockAction(x, y, z));
        while (recentBlockPlacements.size() > 20) recentBlockPlacements.poll();
    }

    public void recordBlockBreak(int x, int y, int z) {
        recentBlockBreaks.offer(new BlockAction(x, y, z));
        while (recentBlockBreaks.size() > 20) recentBlockBreaks.poll();
    }

    public boolean hasRecentBlockPlace(int x, int y, int z, long maxAgeMs) {
        long now = System.currentTimeMillis();
        for (BlockAction a : recentBlockPlacements) {
            if (a.x == x && a.y == y && a.z == z && (now - a.timestamp) < maxAgeMs) return true;
        }
        return false;
    }

    public boolean hasRecentBlockBreak(int x, int y, int z, long maxAgeMs) {
        long now = System.currentTimeMillis();
        for (BlockAction a : recentBlockBreaks) {
            if (a.x == x && a.y == y && a.z == z && (now - a.timestamp) < maxAgeMs) return true;
        }
        return false;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        while (!recentBlockPlacements.isEmpty() && (now - recentBlockPlacements.peek().timestamp) > 5000) recentBlockPlacements.poll();
        while (!recentBlockBreaks.isEmpty() && (now - recentBlockBreaks.peek().timestamp) > 5000) recentBlockBreaks.poll();
    }
}
