package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.TransactionCheck;
import io.windfall.anticheat.core.compensation.TransactionManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class TransactionDetectionTest extends CheckTestBase {

    private TransactionCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new TransactionCheck();
        player = createMockPlayer("TestPlayer");
        TransactionManager txManager = new TransactionManager(testMod);
        Field txField = io.windfall.anticheat.WindfallMod.class.getDeclaredField("transactionManager");
        txField.setAccessible(true);
        txField.set(testMod, txManager);
    }

    private void addPendingTransaction(TransactionManager txManager, WindfallPlayer p) throws Exception {
        Class<?> tsClass = Class.forName("io.windfall.anticheat.core.compensation.TransactionManager$TransactionState");
        java.lang.reflect.Constructor<?> tsCtor = tsClass.getDeclaredConstructor();
        tsCtor.setAccessible(true);
        Object ts = tsCtor.newInstance();

        Field pendingField = tsClass.getDeclaredField("pendingTransactions");
        pendingField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentLinkedQueue<Object> pendingQueue = (ConcurrentLinkedQueue<Object>) pendingField.get(ts);

        Class<?> ptClass = Class.forName("io.windfall.anticheat.core.compensation.TransactionManager$PendingTransaction");
        java.lang.reflect.Constructor<?> ptCtor = ptClass.getDeclaredConstructor(short.class, long.class);
        ptCtor.setAccessible(true);
        Object pt = ptCtor.newInstance((short) 1, System.nanoTime());
        pendingQueue.add(pt);

        Field playerTxField = TransactionManager.class.getDeclaredField("playerTransactions");
        playerTxField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> playerTxMap = (Map<UUID, Object>) playerTxField.get(txManager);
        playerTxMap.put(p.getUuid(), ts);
    }

    @Test
    void normalTick_noFlag() {
        check.onTick(player);
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void normalPing_decreasesBuffer() throws Exception {
        player.setTransactionPing(50);
        check.increaseBuffer(player, 2.0);
        check.onTick(player);
        assertTrue(check.getBuffer(player) < 2.0);
    }

    @Test
    void negativePing_increasesSkipCount() throws Exception {
        player.setTransactionPing(-1);
        TransactionManager txManager = testMod.getTransactionManager();
        addPendingTransaction(txManager, player);
        for (int i = 0; i < 15; i++) {
            check.onTick(player);
        }
        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void excessivePing_increasesSkipCount() throws Exception {
        player.setTransactionPing(6000);
        TransactionManager txManager = testMod.getTransactionManager();
        addPendingTransaction(txManager, player);
        for (int i = 0; i < 15; i++) {
            check.onTick(player);
        }
        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        Field stateField = TransactionCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> states = (Map<UUID, Object>) stateField.get(check);

        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.packet.TransactionCheck$PlayerState");
        Object state = getUnsafe().allocateInstance(stateClass);
        states.put(player.getUuid(), state);
        assertTrue(states.containsKey(player.getUuid()));

        check.removePlayer(player.getUuid());
        assertFalse(states.containsKey(player.getUuid()));
    }
}
