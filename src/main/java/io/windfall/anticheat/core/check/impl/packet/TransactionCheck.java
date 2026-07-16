package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.compensation.TransactionManager;
import io.windfall.anticheat.core.player.WindfallPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Transaction A", stableKey="windfall.packet.transaction", decay=0.005, setbackVl=15,
    compat={CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier=1.3)
public class TransactionCheck extends Check implements PacketCheck {

    private static final int SKIP_THRESHOLD = 10;
    private static final int UNKNOWN_THRESHOLD = 5;
    private static final long WINDOW_MS = 5000;

    private static final class PlayerState {
        int skippedInWindow;
        int unknownInWindow;
        long windowStart;
        int lastPendingCount;
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
        if (!enabled) return;

        TransactionManager txManager = WindfallMod.getInstance().getTransactionManager();
        if (txManager == null) return;

        if (packet instanceof net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket keepAlive) {
            handleTransactionResponse(player, txManager, keepAlive);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private void handleTransactionResponse(WindfallPlayer player, TransactionManager txManager, net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket keepAlive) {
        short responseId = (short) keepAlive.getId();

        int prevUnknown = txManager.getUnknownResponses();
        txManager.processTransaction(player, responseId);
        int newUnknown = txManager.getUnknownResponses();

        if (newUnknown > prevUnknown) {
            PlayerState state = getState(player);
            resetWindowIfNeeded(state);
            state.unknownInWindow++;
            evaluatePlayer(player, state);
        }
    }

    public void onTick(WindfallPlayer player) {
        TransactionManager txManager = WindfallMod.getInstance().getTransactionManager();
        if (txManager == null) return;

        PlayerState state = getState(player);
        resetWindowIfNeeded(state);

        int currentPending = txManager.getPendingCount(player.getUuid());

        if (state.lastPendingCount > 0 && currentPending >= state.lastPendingCount) {
            state.skippedInWindow++;
            txManager.incrementSkippedTransactions();
        }

        state.lastPendingCount = currentPending;
        evaluatePlayer(player, state);
    }

    private void evaluatePlayer(WindfallPlayer player, PlayerState state) {
        int totalAnomalies = state.skippedInWindow + state.unknownInWindow;

        if (state.skippedInWindow >= SKIP_THRESHOLD) {
            increaseBuffer(player, 1.5);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
                state.skippedInWindow = 0;
                state.unknownInWindow = 0;
            }
        } else if (state.unknownInWindow >= UNKNOWN_THRESHOLD) {
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
                state.skippedInWindow = 0;
                state.unknownInWindow = 0;
            }
        } else if (totalAnomalies == 0) {
            decreaseBuffer(player, 0.2);
        }
    }

    private void resetWindowIfNeeded(PlayerState state) {
        long now = System.currentTimeMillis();
        if (state.windowStart == 0 || now - state.windowStart > WINDOW_MS) {
            state.skippedInWindow = 0;
            state.unknownInWindow = 0;
            state.windowStart = now;
        }
    }
}
