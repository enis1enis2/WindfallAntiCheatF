package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.FastBreakCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class FastBreakDetectionTest extends CheckTestBase {

    private FastBreakCheck createCheck() { return new FastBreakCheck(); }

    private Object getOrCreateState(FastBreakCheck check, UUID uuid) throws Exception {
        Field stateField = FastBreakCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> states = (Map<UUID, Object>) stateField.get(check);
        if (states.containsKey(uuid)) return states.get(uuid);
        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.movement.FastBreakCheck$BreakState");
        java.lang.reflect.Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object state = ctor.newInstance();
        states.put(uuid, state);
        return state;
    }

    @Test
    void constructor_readCheckData() {
        FastBreakCheck check = createCheck();
        assertEquals("FastBreak A", check.getName());
        assertEquals("windfall.movement.fastbreak", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        FastBreakCheck check = createCheck();
        Field field = FastBreakCheck.class.getDeclaredField("playerStates");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        FastBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        getOrCreateState(check, playerA.getUuid());
        getOrCreateState(check, playerB.getUuid());

        Field stateField = FastBreakCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, ?> states = (Map<UUID, ?>) stateField.get(check);
        assertEquals(2, states.size());
    }

    @Test
    void perPlayerState_startTimestampIsPerPlayer() throws Exception {
        FastBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid());
        Object stateB = getOrCreateState(check, playerB.getUuid());

        Field timeField = stateA.getClass().getDeclaredField("startTimestamp");
        timeField.setAccessible(true);
        timeField.setLong(stateA, 12345L);

        assertEquals(0L, timeField.getLong(stateB));
    }

    @Test
    void buffers_arePerPlayer() {
        FastBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 5.0);

        assertEquals(5.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
