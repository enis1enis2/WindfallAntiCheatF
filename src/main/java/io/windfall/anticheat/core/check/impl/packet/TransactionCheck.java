package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Transaction A", stableKey="windfall.packet.transaction", decay=0.02, setbackVl=20)
public class TransactionCheck extends Check implements PacketCheck {
    private int skipCount = 0;

    private final ConcurrentHashMap<java.util.UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerState {
        boolean waitingForTransaction = false;
        long transactionSentTime = 0;
        int transactionId = 0;
        int missedTransactions = 0;
    }

    private PlayerState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        // TransactionConfirmationC2SPacket was removed in 1.21.5.
        // Ping is now tracked via server-side keepalive or the pingPongManager.
        // This check monitors for anomalously stale ping values.
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public void onTick(WindfallPlayer player) {
        int ping = player.getTransactionPing();
        if (ping < 0 || ping > 5000) {
            skipCount++;
            if (skipCount > 20) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            skipCount = 0;
            decreaseBuffer(player, 0.5);
        }
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid);
    }
}
