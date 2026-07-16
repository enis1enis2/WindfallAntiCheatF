package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects timer-based speed hacks and slow hacks by measuring the rate of movement packets
 * per tick over a sliding window.
 *
 * <p>Algorithm: Movement packets are timestamped and stored in a sliding window of
 * {@value WINDOW_DURATION_MS}ms ({@value WINDOW_TICK_COUNT} ticks at 20 TPS). The packets-per-tick
 * ratio is compared against dynamic thresholds that account for latency jitter:
 * <ul>
 *   <li><b>Speed hack</b>: packets/tick exceeds (maxNormal + jitter) × {@value SPEEDHACK_MULTIPLIER}</li>
 *   <li><b>Slow hack</b>: packets/tick falls below minNormal × {@value SLOWHACK_MULTIPLIER}</li>
 * </ul>
 *
 * <p>Jitter tolerance adapts to the player's ping: {@code max(2, ceil(ping / 50ms))} to prevent
 * false positives on high-latency connections.
 *
 * <p>Double-buffer system: A secondary window is flushed every {@value DOUBLE_BUFFER_INTERVAL_MS}ms
 * to detect bursts of high packet rates that may average out over the full 1-second window.
 *
 * <p>Flags require consecutive violations:
 * <ul>
 *   <li>Speed hack: ≥2 consecutive high windows (or ≥1 if exceed × {@value FLAG_THRESHOLD_MULTIPLIER})</li>
 *   <li>Slow hack: ≥4 consecutive low windows</li>
 * </ul>
 *
 * @see SpeedCheck for horizontal speed validation
 */
@CheckData(name = "Timer A", stableKey = "windfall.movement.timer", decay = 0.005, setbackVl = 25, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class TimerCheck extends Check implements PacketCheck {

    /** Minimum normal packets per tick (a still player sends roughly 1 flying packet/tick) */
    private static final int NORMAL_PACKETS_PER_TICK_MIN = 1;
    /** Maximum normal packets per tick under normal conditions (flying + position updates) */
    private static final int NORMAL_PACKETS_PER_TICK_MAX = 3;
    /** Base latency jitter tolerance in packets — prevents false flags from network timing variance */
    private static final int LATENCY_JITTER_TOLERANCE = 2;
    /** Sliding window duration in milliseconds — 1 second = 20 ticks at 20 TPS */
    private static final long WINDOW_DURATION_MS = 1000;
    /** Expected number of ticks within the window — used to compute packets-per-tick */
    private static final long WINDOW_TICK_COUNT = 20;
    /** Multiplier above the adjusted max-normal rate that indicates a speed hack */
    private static final double SPEEDHACK_MULTIPLIER = 1.2;
    /** Multiplier below the min-normal rate that indicates a slow hack */
    private static final double SLOWHACK_MULTIPLIER = 0.5;
    /** Additional multiplier on top of speedHackThreshold for "severe" speed hack detection */
    private static final double FLAG_THRESHOLD_MULTIPLIER = 1.5;
    /** Interval (ms) between secondary buffer flushes — catches short burst speed hacks */
    private static final long DOUBLE_BUFFER_INTERVAL_MS = 500;

    private static final class PlayerState {
        ArrayDeque<Long> packetTimestamps = new ArrayDeque<>();
        ArrayDeque<Long> secondaryPacketTimestamps = new ArrayDeque<>();
        long lastSecondaryFlush = System.currentTimeMillis();
        int consecutiveHighWindows;
        int consecutiveLowWindows;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;

        PlayerState state = getState(player);
        long now = System.currentTimeMillis();

        state.packetTimestamps.addLast(now);
        state.secondaryPacketTimestamps.addLast(now);

        cleanWindow(state.packetTimestamps, now);
        cleanWindow(state.secondaryPacketTimestamps, now);

        double packetsPerTick = state.packetTimestamps.size() / (double) WINDOW_TICK_COUNT;

        int jitterTolerance = Math.max(LATENCY_JITTER_TOLERANCE,
                (int) Math.ceil(player.getTransactionPing() / 50.0));

        int maxNormalPerTick = NORMAL_PACKETS_PER_TICK_MAX + jitterTolerance;
        double speedHackThreshold = maxNormalPerTick * SPEEDHACK_MULTIPLIER;
        double slowHackThreshold = Math.max(0.2, NORMAL_PACKETS_PER_TICK_MIN * SLOWHACK_MULTIPLIER);

        if (packetsPerTick > speedHackThreshold) {
            if (packetsPerTick > speedHackThreshold * FLAG_THRESHOLD_MULTIPLIER) {
                state.consecutiveHighWindows++;
            } else {
                state.consecutiveHighWindows = Math.max(0, state.consecutiveHighWindows - 1);
            }

            if (state.consecutiveHighWindows >= 2) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                    state.consecutiveHighWindows = 0;
                }
            }
        } else if (packetsPerTick < slowHackThreshold) {
            state.consecutiveLowWindows++;
            if (state.consecutiveLowWindows >= 4) {
                increaseBuffer(player, 0.8);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                    state.consecutiveLowWindows = 0;
                }
            }
        } else {
            state.consecutiveHighWindows = Math.max(0, state.consecutiveHighWindows - 1);
            state.consecutiveLowWindows = Math.max(0, state.consecutiveLowWindows - 1);
            decreaseBuffer(player, 0.1);
        }

        if (now - state.lastSecondaryFlush >= DOUBLE_BUFFER_INTERVAL_MS) {
            long secondaryCount = state.secondaryPacketTimestamps.stream()
                    .filter(t -> now - t <= DOUBLE_BUFFER_INTERVAL_MS)
                    .count();
            double secondaryRate = secondaryCount / (DOUBLE_BUFFER_INTERVAL_MS / 50.0);

            if (secondaryRate > speedHackThreshold * 1.3) {
                increaseBuffer(player, 0.5);
            }

            state.secondaryPacketTimestamps.clear();
            state.lastSecondaryFlush = now;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    /** Called from CheckManager each tick — no-op since this check uses timestamp-based sliding window. */
    public void onTick(WindfallPlayer player) {
    }

    private void cleanWindow(ArrayDeque<Long> window, long now) {
        Iterator<Long> it = window.iterator();
        while (it.hasNext()) {
            if (now - it.next() > WINDOW_DURATION_MS) {
                it.remove();
            } else {
                break;
            }
        }
    }
}
