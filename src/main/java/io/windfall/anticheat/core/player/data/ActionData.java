package io.windfall.anticheat.core.player.data;

import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Tracks block-level actions to provide exemptions for movement checks.
 *
 * <p>Monitors block placement, block breaking, piston pushes, and server-side block
 * updates under the player's feet. Movement checks use these tick counters to
 * temporarily widen tolerance after world changes that can cause legitimate
 * desync between client and server positions.</p>
 *
 * <p>Key exemption methods:
 * <ul>
 *   <li>{@link #hasRecentPistonUpdate(int)} — piston pushed a block near the player</li>
 *   <li>{@link #hasRecentBlockUpdateUnder(int)} — server sent a block change under the player</li>
 *   <li>{@link #hasRecentBlockPlaceAttempt(int)} — a placed block was confirmed under the player</li>
 * </ul>
 *
 * <p>Thread safety: tick counters are plain ints updated only from the main thread
 * via packet callbacks. Checks read these from Netty threads, but stale reads
 * are acceptable (worst case: one extra tick of tolerance).</p>
 */
public class ActionData {
    private static final int MAX_TICKS = 1000;

    private final WindfallPlayer player;
    private final ConcurrentLinkedQueue<BlockAction> recentBlockPlacements = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<BlockAction> recentBlockBreaks = new ConcurrentLinkedQueue<>();

    private volatile boolean usingItem;
    private volatile long useItemStart;

    private int sincePistonUpdateTicks = MAX_TICKS;
    private int sinceBlockUpdateUnderTicks = MAX_TICKS;
    private int sinceBlockPlaceAttemptTicks = MAX_TICKS;

    static class BlockAction {
        final int x, y, z;
        final long timestamp;
        BlockAction(int x, int y, int z) {
            this.x = x; this.y = y; this.z = z;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public ActionData(WindfallPlayer player) { this.player = player; }

    // ========================================================================
    // Public query methods — called by movement checks for exemptions
    // ========================================================================

    public boolean isUsingItem() { return usingItem; }
    public void setUsingItem(boolean using) { this.usingItem = using; if (using) useItemStart = System.currentTimeMillis(); }
    public long getUseItemStart() { return useItemStart; }

    /**
     * Returns true if a piston-related block update occurred near the player
     * within the given number of ticks.
     */
    public boolean hasRecentPistonUpdate(int ticks) {
        return sincePistonUpdateTicks <= ticks;
    }

    /**
     * Returns true if a server-side block change under the player occurred
     * within the given number of ticks.
     */
    public boolean hasRecentBlockUpdateUnder(int ticks) {
        return sinceBlockUpdateUnderTicks <= ticks;
    }

    /**
     * Returns true if the player attempted to place a block (sent the packet)
     * within the given number of ticks, regardless of server confirmation.
     */
    public boolean hasRecentBlockPlaceAttempt(int ticks) {
        return sinceBlockPlaceAttemptTicks <= ticks;
    }

    // ========================================================================
    // Notification methods — called from PacketListener
    // ========================================================================

    public void notifyPistonUpdate() {
        sincePistonUpdateTicks = 0;
    }

    public void notifyBlockUpdateUnder() {
        sinceBlockUpdateUnderTicks = 0;
    }

    public void notifyBlockPlaceAttempt() {
        sinceBlockPlaceAttemptTicks = 0;
    }

    // ========================================================================
    // Block action tracking (queue-based)
    // ========================================================================

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

    // ========================================================================
    // Tick management — called from CheckManager
    // ========================================================================

    public void tick() {
        sincePistonUpdateTicks = Math.min(sincePistonUpdateTicks + 1, MAX_TICKS);
        sinceBlockUpdateUnderTicks = Math.min(sinceBlockUpdateUnderTicks + 1, MAX_TICKS);
        sinceBlockPlaceAttemptTicks = Math.min(sinceBlockPlaceAttemptTicks + 1, MAX_TICKS);

        long now = System.currentTimeMillis();
        while (!recentBlockPlacements.isEmpty() && (now - recentBlockPlacements.peek().timestamp) > 5000) recentBlockPlacements.poll();
        while (!recentBlockBreaks.isEmpty() && (now - recentBlockBreaks.peek().timestamp) > 5000) recentBlockBreaks.poll();
    }
}
