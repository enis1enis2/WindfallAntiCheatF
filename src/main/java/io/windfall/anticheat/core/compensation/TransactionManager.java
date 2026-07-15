package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionManager {
    private final WindfallMod mod;
    private final Map<UUID, TransactionEntry> pendingTransactions = new ConcurrentHashMap<>();
    private int nextTransactionId = 1;

    static class TransactionEntry {
        final short id;
        final long sendTime;
        TransactionEntry(short id) {
            this.id = id;
            this.sendTime = System.nanoTime();
        }
    }

    public TransactionManager(WindfallMod mod) { this.mod = mod; }

    public void sendTransaction(WindfallPlayer player) {
        if (player == null || player.getServerPlayer() == null) return;
        short id = (short) (nextTransactionId++ & 0xFFFF);
        pendingTransactions.put(player.getUuid(), new TransactionEntry(id));
        try {
            player.getServerPlayer().networkHandler.sendPacket(new KeepAliveS2CPacket(id));
        } catch (Exception e) {
            // Player may have disconnected
        }
    }

    public void processTransaction(WindfallPlayer player, short id) {
        TransactionEntry entry = pendingTransactions.remove(player.getUuid());
        if (entry != null) {
            long elapsed = System.nanoTime() - entry.sendTime;
            int pingMs = (int) (elapsed / 1_000_000);
            player.setTransactionPing(pingMs);
            player.setTransactionId(id);
        }
    }

    public void onPlayerQuit(UUID uuid) {
        pendingTransactions.remove(uuid);
    }
}
