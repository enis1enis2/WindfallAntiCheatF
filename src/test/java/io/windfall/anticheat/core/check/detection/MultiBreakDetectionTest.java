package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.MultiBreakCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class MultiBreakDetectionTest extends CheckTestBase {

    private MultiBreakCheck createCheck() { return new MultiBreakCheck(); }

    private Object getOrCreateState(MultiBreakCheck check, String uuidStr) throws Exception {
        Field stateField = MultiBreakCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> states = (Map<String, Object>) stateField.get(check);
        if (states.containsKey(uuidStr)) return states.get(uuidStr);
        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.movement.MultiBreakCheck$PlayerState");
        java.lang.reflect.Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object state = ctor.newInstance();
        states.put(uuidStr, state);
        return state;
    }

    @Test
    void constructor_readCheckData() {
        MultiBreakCheck check = createCheck();
        assertEquals("MultiBreak A", check.getName());
        assertEquals("windfall.movement.multibreak", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        MultiBreakCheck check = createCheck();
        Field field = MultiBreakCheck.class.getDeclaredField("playerStates");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_breaksThisTickIsPerPlayer() throws Exception {
        MultiBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid().toString());
        Object stateB = getOrCreateState(check, playerB.getUuid().toString());

        Field breaksField = stateA.getClass().getDeclaredField("breaksThisTick");
        breaksField.setAccessible(true);
        breaksField.setInt(stateA, 5);

        assertEquals(0, breaksField.getInt(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        MultiBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        getOrCreateState(check, playerA.getUuid().toString());
        getOrCreateState(check, playerB.getUuid().toString());

        Field stateField = MultiBreakCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> states = (Map<String, ?>) stateField.get(check);
        assertEquals(2, states.size());
    }

    @Test
    void buffers_arePerPlayer() {
        MultiBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 1.5);

        assertEquals(1.5, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
