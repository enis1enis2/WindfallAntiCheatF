package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.MultiPlaceCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class MultiPlaceDetectionTest extends CheckTestBase {

    private MultiPlaceCheck createCheck() { return new MultiPlaceCheck(); }

    private Object getOrCreateState(MultiPlaceCheck check, String uuidStr) throws Exception {
        Field stateField = MultiPlaceCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> states = (Map<String, Object>) stateField.get(check);
        if (states.containsKey(uuidStr)) return states.get(uuidStr);
        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.movement.MultiPlaceCheck$PlayerState");
        java.lang.reflect.Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object state = ctor.newInstance();
        states.put(uuidStr, state);
        return state;
    }

    @Test
    void constructor_readCheckData() {
        MultiPlaceCheck check = createCheck();
        assertEquals("MultiPlace A", check.getName());
        assertEquals("windfall.movement.multiplace", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        MultiPlaceCheck check = createCheck();
        Field field = MultiPlaceCheck.class.getDeclaredField("playerStates");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_placesThisTickIsPerPlayer() throws Exception {
        MultiPlaceCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid().toString());
        Object stateB = getOrCreateState(check, playerB.getUuid().toString());

        Field placesField = stateA.getClass().getDeclaredField("placesThisTick");
        placesField.setAccessible(true);
        placesField.setInt(stateA, 10);

        assertEquals(0, placesField.getInt(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        MultiPlaceCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        getOrCreateState(check, playerA.getUuid().toString());
        getOrCreateState(check, playerB.getUuid().toString());

        Field stateField = MultiPlaceCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> states = (Map<String, ?>) stateField.get(check);
        assertEquals(2, states.size());
    }

    @Test
    void buffers_arePerPlayer() {
        MultiPlaceCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 4.0);

        assertEquals(4.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
