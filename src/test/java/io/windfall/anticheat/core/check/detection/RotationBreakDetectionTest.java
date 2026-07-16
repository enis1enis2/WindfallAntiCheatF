package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.RotationBreakCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class RotationBreakDetectionTest extends CheckTestBase {

    private RotationBreakCheck createCheck() { return new RotationBreakCheck(); }

    private Object getOrCreateState(RotationBreakCheck check, String uuidStr) throws Exception {
        Field stateField = RotationBreakCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> states = (Map<String, Object>) stateField.get(check);
        if (states.containsKey(uuidStr)) return states.get(uuidStr);
        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.movement.RotationBreakCheck$PlayerState");
        java.lang.reflect.Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object state = ctor.newInstance();
        states.put(uuidStr, state);
        return state;
    }

    @Test
    void constructor_readCheckData() {
        RotationBreakCheck check = createCheck();
        assertEquals("RotationBreak A", check.getName());
        assertEquals("windfall.movement.rotationbreak", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        RotationBreakCheck check = createCheck();
        Field field = RotationBreakCheck.class.getDeclaredField("playerStates");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_trackingBreakIsPerPlayer() throws Exception {
        RotationBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid().toString());
        Object stateB = getOrCreateState(check, playerB.getUuid().toString());

        Field trackingField = stateA.getClass().getDeclaredField("trackingBreak");
        trackingField.setAccessible(true);
        trackingField.setBoolean(stateA, true);

        assertFalse(trackingField.getBoolean(stateB));
    }

    @Test
    void perPlayerState_lastYawIsPerPlayer() throws Exception {
        RotationBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid().toString());
        Object stateB = getOrCreateState(check, playerB.getUuid().toString());

        Field yawField = stateA.getClass().getDeclaredField("lastYaw");
        yawField.setAccessible(true);
        yawField.setFloat(stateA, 90.0f);

        assertEquals(0.0f, yawField.getFloat(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        RotationBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        getOrCreateState(check, playerA.getUuid().toString());
        getOrCreateState(check, playerB.getUuid().toString());

        Field stateField = RotationBreakCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> states = (Map<String, ?>) stateField.get(check);
        assertEquals(2, states.size());
    }

    @Test
    void buffers_arePerPlayer() {
        RotationBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 2.5);

        assertEquals(2.5, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
