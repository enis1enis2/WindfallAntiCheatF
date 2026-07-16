package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.player.WindfallPlayer;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PingPongManager {

    private final WindfallMod mod;
    private final Map<UUID, PlayerPingState> playerStates = new ConcurrentHashMap<>();

    public PingPongManager(WindfallMod mod) {
        this.mod = mod;
    }

    public void onTickStart(WindfallPlayer player) {
        if (!player.isValid()) return;
        PlayerPingState state = getState(player);
        state.sendPing(mod, player, true);
    }

    public void onTickEnd(WindfallPlayer player) {
        if (!player.isValid()) return;
        PlayerPingState state = getState(player);
        state.currentTick++;
        state.sendPing(mod, player, false);
    }

    public boolean processPingResponse(WindfallPlayer player, short id) {
        PlayerPingState state = playerStates.get(player.getUuid());
        if (state == null) return false;

        PingRecord record = state.pendingPings.remove(id);
        if (record == null) return false;

        if (record.isStartPing) {
            state.confirmedTick = Math.max(state.confirmedTick, record.tick - 1);
        } else {
            state.confirmedTick = Math.max(state.confirmedTick, record.tick);
        }

        Queue<Runnable> callbacks;
        while ((callbacks = state.tickCallbacks.poll()) != null) {
            for (Runnable cb : callbacks) {
                try {
                    cb.run();
                } catch (Exception e) {
                }
            }
        }

        return true;
    }

    public void onTickConfirmed(WindfallPlayer player, int tick, Runnable callback) {
        PlayerPingState state = getState(player);
        if (tick <= state.confirmedTick) {
            try {
                callback.run();
            } catch (Exception e) {
            }
            return;
        }
        Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
        queue.add(callback);
        state.tickCallbacks.add(queue);
    }

    public int getConfirmedTick(WindfallPlayer player) {
        PlayerPingState state = playerStates.get(player.getUuid());
        return state != null ? state.confirmedTick : 0;
    }

    public boolean isTickConfirmed(WindfallPlayer player, int tick) {
        return getConfirmedTick(player) >= tick;
    }

    public int getEstimatedLatencyMs(WindfallPlayer player) {
        PlayerPingState state = playerStates.get(player.getUuid());
        if (state == null) return 0;
        return player.getTransactionPing();
    }

    public int getCurrentTick(WindfallPlayer player) {
        PlayerPingState state = playerStates.get(player.getUuid());
        return state != null ? state.currentTick : 0;
    }

    public void onPlayerQuit(UUID uuid) {
        playerStates.remove(uuid);
    }

    private PlayerPingState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerPingState());
    }

    private static final class PlayerPingState {
        volatile int currentTick;
        volatile int confirmedTick;
        final Map<Short, PingRecord> pendingPings = new ConcurrentHashMap<>();
        final Queue<Queue<Runnable>> tickCallbacks = new ConcurrentLinkedQueue<>();

        void sendPing(WindfallMod mod, WindfallPlayer player, boolean isStartPing) {
            try {
                TransactionManager txMgr = mod.getTransactionManager();
                short id = txMgr.sendPingPongTransaction(player);
                if (id >= 0) {
                    pendingPings.put(id, new PingRecord(currentTick, isStartPing));
                }
            } catch (Exception e) {
            }
        }
    }

    private static final class PingRecord {
        final int tick;
        final boolean isStartPing;

        PingRecord(int tick, boolean isStartPing) {
            this.tick = tick;
            this.isStartPing = isStartPing;
        }
    }
}
