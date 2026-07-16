package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.player.WindfallPlayer;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class TransactionManager {

    private final WindfallMod mod;
    private final Map<UUID, TransactionState> playerTransactions = new ConcurrentHashMap<>();

    private final AtomicInteger skippedTransactions = new AtomicInteger(0);
    private final AtomicInteger unknownResponses = new AtomicInteger(0);
    private final AtomicInteger noResponseCount = new AtomicInteger(0);

    public TransactionManager(WindfallMod mod) {
        this.mod = mod;
    }

    public void sendTransaction(WindfallPlayer player) {
        if (player == null || player.getServerPlayer() == null) return;
        if (!player.isValid()) return;

        TransactionState state = playerTransactions.computeIfAbsent(
                player.getUuid(), k -> new TransactionState());

        short id = state.nextTransactionId();
        long sendTime = System.nanoTime();

        state.pendingTransactions.add(new PendingTransaction(id, sendTime));

        try {
            net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket packet =
                new net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket(id);
            player.getServerPlayer().networkHandler.sendPacket(packet);
        } catch (Exception e) {
            WindfallMod.LOGGER.warn("Windfall: Failed to send transaction packet: " + e.getMessage());
        }
    }

    public void processTransaction(WindfallPlayer player, short id) {
        TransactionState state = playerTransactions.get(player.getUuid());
        if (state == null) return;

        long receiveTime = System.nanoTime();
        PendingTransaction matched = null;

        Queue<PendingTransaction> remaining = new ConcurrentLinkedQueue<>();
        while (!state.pendingTransactions.isEmpty()) {
            PendingTransaction tx = state.pendingTransactions.poll();
            if (tx.id == id) {
                matched = tx;
            } else {
                remaining.add(tx);
            }
        }
        state.pendingTransactions.addAll(remaining);

        if (matched != null) {
            long pingNanos = receiveTime - matched.sendTime;
            player.setTransactionPing((int) (pingNanos / 1_000_000));
        } else {
            unknownResponses.incrementAndGet();
        }

        Runnable callback = state.callbacks.remove(id);
        if (callback != null) {
            try {
                callback.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addCallback(WindfallPlayer player, short transactionId, Runnable callback) {
        TransactionState state = playerTransactions.computeIfAbsent(
                player.getUuid(), k -> new TransactionState());
        state.callbacks.put(transactionId, callback);
    }

    public short sendTransactionWithCallback(WindfallPlayer player, Runnable callback) {
        TransactionState state = playerTransactions.computeIfAbsent(
                player.getUuid(), k -> new TransactionState());

        short id = state.nextTransactionId();
        long sendTime = System.nanoTime();

        state.pendingTransactions.add(new PendingTransaction(id, sendTime));
        state.callbacks.put(id, callback);

        try {
            net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket packet =
                new net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket(id);
            player.getServerPlayer().networkHandler.sendPacket(packet);
        } catch (Exception e) {
            WindfallMod.LOGGER.warn("Windfall: Failed to send transaction packet: " + e.getMessage());
        }

        return id;
    }

    public short sendPingPongTransaction(WindfallPlayer player) {
        if (player == null || player.getServerPlayer() == null) return -1;
        if (!player.isValid()) return -1;

        TransactionState state = playerTransactions.computeIfAbsent(
                player.getUuid(), k -> new TransactionState());

        short id = state.nextTransactionId();
        long sendTime = System.nanoTime();

        state.pendingTransactions.add(new PendingTransaction(id, sendTime));

        try {
            net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket packet =
                new net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket(id);
            player.getServerPlayer().networkHandler.sendPacket(packet);
            return id;
        } catch (Exception e) {
            WindfallMod.LOGGER.warn("Windfall: Failed to send ping-pong transaction: " + e.getMessage());
            return -1;
        }
    }

    public void onPlayerQuit(UUID uuid) {
        playerTransactions.remove(uuid);
    }

    public void incrementSkippedTransactions() {
        skippedTransactions.incrementAndGet();
    }

    public void incrementNoResponseCount() {
        noResponseCount.incrementAndGet();
    }

    public int getSkippedTransactions() {
        return skippedTransactions.get();
    }

    public int getUnknownResponses() {
        return unknownResponses.get();
    }

    public int getNoResponseCount() {
        return noResponseCount.get();
    }

    public int getPendingCount(UUID uuid) {
        TransactionState state = playerTransactions.get(uuid);
        return state != null ? state.pendingTransactions.size() : 0;
    }

    private static final class PendingTransaction {
        final short id;
        final long sendTime;

        PendingTransaction(short id, long sendTime) {
            this.id = id;
            this.sendTime = sendTime;
        }
    }

    private static final class TransactionState {
        final Queue<PendingTransaction> pendingTransactions = new ConcurrentLinkedQueue<>();
        final Map<Short, Runnable> callbacks = new ConcurrentHashMap<>();
        final AtomicInteger transactionCounter = new AtomicInteger(0);

        short nextTransactionId() {
            return (short) (transactionCounter.incrementAndGet() & 0x7FFF);
        }
    }
}
