package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.MultiPlaceCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class MultiPlaceDetectionTest extends CheckTestBase {

    private MultiPlaceCheck createCheck() { return new MultiPlaceCheck(); }

    private Object getOrCreateState(MultiPlaceCheck check, UUID uuid) throws Exception {
        Field stateField = MultiPlaceCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> states = (Map<UUID, Object>) stateField.get(check);
        if (states.containsKey(uuid)) return states.get(uuid);
        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.movement.MultiPlaceCheck$PlayerState");
        java.lang.reflect.Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object state = ctor.newInstance();
        states.put(uuid, state);
        return state;
    }

    @Test
    void constructor_readCheckData() {
        MultiPlaceCheck check = createCheck();
        assertEquals("Multi Place", check.getName());
        assertEquals("windfall.movement.multiplace", check.getStableKey());
        assertEquals(10, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        MultiPlaceCheck check = createCheck();
        Field field = MultiPlaceCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_placesThisTickIsPerPlayer() throws Exception {
        MultiPlaceCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid());
        Object stateB = getOrCreateState(check, playerB.getUuid());

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

        getOrCreateState(check, playerA.getUuid());
        getOrCreateState(check, playerB.getUuid());

        Field stateField = MultiPlaceCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, ?> states = (Map<UUID, ?>) stateField.get(check);
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
