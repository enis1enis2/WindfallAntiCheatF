package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.SpeedCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class SpeedDetectionTest extends CheckTestBase {

    private SpeedCheck createCheck() { return new SpeedCheck(); }

    private Object getOrCreateState(SpeedCheck check, UUID uuid) throws Exception {
        Field stateField = SpeedCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> states = (Map<String, Object>) stateField.get(check);
        String key = uuid.toString();
        if (states.containsKey(key)) return states.get(key);
        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.movement.SpeedCheck$PlayerState");
        java.lang.reflect.Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object state = ctor.newInstance();
        states.put(key, state);
        return state;
    }

    @Test
    void constructor_readCheckData() {
        SpeedCheck check = createCheck();
        assertEquals("Speed A", check.getName());
        assertEquals("windfall.movement.speed", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        SpeedCheck check = createCheck();
        Field field = SpeedCheck.class.getDeclaredField("playerStates");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_maxObservedSpeedIsPerPlayer() throws Exception {
        SpeedCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid());
        Object stateB = getOrCreateState(check, playerB.getUuid());

        Field maxSpeedField = stateA.getClass().getDeclaredField("maxObservedSpeed");
        maxSpeedField.setAccessible(true);
        maxSpeedField.setDouble(stateA, 0.5);

        assertEquals(0.0, maxSpeedField.getDouble(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        SpeedCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        getOrCreateState(check, playerA.getUuid());
        getOrCreateState(check, playerB.getUuid());

        Field stateField = SpeedCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> states = (Map<String, ?>) stateField.get(check);
        assertEquals(2, states.size());
    }

    @Test
    void buffers_arePerPlayer() {
        SpeedCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 4.0);

        assertEquals(4.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
