package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.FlightCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class FlightDetectionTest extends CheckTestBase {

    private FlightCheck createCheck() { return new FlightCheck(); }

    private Object getOrCreateState(FlightCheck check, UUID uuid) throws Exception {
        Field stateField = FlightCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> states = (Map<String, Object>) stateField.get(check);
        String key = uuid.toString();
        if (states.containsKey(key)) return states.get(key);
        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.movement.FlightCheck$PlayerState");
        java.lang.reflect.Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object state = ctor.newInstance();
        states.put(key, state);
        return state;
    }

    @Test
    void constructor_readCheckData() {
        FlightCheck check = createCheck();
        assertEquals("Flight A", check.getName());
        assertEquals("windfall.movement.fly", check.getStableKey());
        assertEquals(15, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        FlightCheck check = createCheck();
        Field field = FlightCheck.class.getDeclaredField("playerStates");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_expectedDeltaYIsPerPlayer() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid());
        Object stateB = getOrCreateState(check, playerB.getUuid());

        Field deltaYField = stateA.getClass().getDeclaredField("expectedDeltaY");
        deltaYField.setAccessible(true);
        deltaYField.setDouble(stateA, -0.08);

        assertEquals(0.0, deltaYField.getDouble(stateB));
    }

    @Test
    void perPlayerState_hoverTicksIsPerPlayer() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid());
        Object stateB = getOrCreateState(check, playerB.getUuid());

        Field hoverTicksField = stateA.getClass().getDeclaredField("hoverTicks");
        hoverTicksField.setAccessible(true);
        hoverTicksField.setInt(stateA, 25);

        assertEquals(0, hoverTicksField.getInt(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        FlightCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        getOrCreateState(check, playerA.getUuid());
        getOrCreateState(check, playerB.getUuid());

        Field stateField = FlightCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> states = (Map<String, ?>) stateField.get(check);
        assertEquals(2, states.size());
    }

    @Test
    void buffers_arePerPlayer() {
        FlightCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 3.5);

        assertEquals(3.5, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
