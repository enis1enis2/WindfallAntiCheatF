package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.TransactionCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class TransactionDetectionTest extends CheckTestBase {

    private TransactionCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new TransactionCheck();
        player = createMockPlayer("TestPlayer");
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
        for (int i = 0; i < 25; i++) {
            check.onTick(player);
        }
        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void excessivePing_increasesSkipCount() throws Exception {
        player.setTransactionPing(6000);
        for (int i = 0; i < 25; i++) {
            check.onTick(player);
        }
        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        Field stateField = TransactionCheck.class.getDeclaredField("playerStates");
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
