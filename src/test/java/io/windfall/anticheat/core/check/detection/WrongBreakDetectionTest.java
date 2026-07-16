package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.WrongBreakCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class WrongBreakDetectionTest extends CheckTestBase {

    private WrongBreakCheck createCheck() { return new WrongBreakCheck(); }

    private Object getOrCreateState(WrongBreakCheck check, UUID uuid) throws Exception {
        Field stateField = WrongBreakCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> states = (Map<UUID, Object>) stateField.get(check);
        if (states.containsKey(uuid)) return states.get(uuid);
        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.movement.WrongBreakCheck$PlayerState");
        java.lang.reflect.Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object state = ctor.newInstance();
        states.put(uuid, state);
        return state;
    }

    @Test
    void constructor_readCheckData() {
        WrongBreakCheck check = createCheck();
        assertEquals("Wrong Break", check.getName());
        assertEquals("windfall.movement.wrongbreak", check.getStableKey());
        assertEquals(10, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        WrongBreakCheck check = createCheck();
        Field field = WrongBreakCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_startBlockPosIsPerPlayer() throws Exception {
        WrongBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid());
        Object stateB = getOrCreateState(check, playerB.getUuid());

        Field posXField = stateA.getClass().getDeclaredField("lastBreakX");
        posXField.setAccessible(true);
        posXField.set(stateA, 100.0);

        Field posYField = stateA.getClass().getDeclaredField("lastBreakY");
        posYField.setAccessible(true);
        posYField.set(stateA, 64.0);

        Field posXFieldB = stateB.getClass().getDeclaredField("lastBreakX");
        posXFieldB.setAccessible(true);
        assertEquals(0.0, posXFieldB.get(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        WrongBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        getOrCreateState(check, playerA.getUuid());
        getOrCreateState(check, playerB.getUuid());

        Field stateField = WrongBreakCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, ?> states = (Map<UUID, ?>) stateField.get(check);
        assertEquals(2, states.size());
    }

    @Test
    void buffers_arePerPlayer() {
        WrongBreakCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 2.0);

        assertEquals(2.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
