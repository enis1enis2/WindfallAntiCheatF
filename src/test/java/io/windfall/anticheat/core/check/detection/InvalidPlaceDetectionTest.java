package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.InvalidPlaceCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class InvalidPlaceDetectionTest extends CheckTestBase {

    private InvalidPlaceCheck createCheck() { return new InvalidPlaceCheck(); }

    private Object getOrCreateState(InvalidPlaceCheck check, UUID uuid) throws Exception {
        Field stateField = InvalidPlaceCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> states = (Map<UUID, Object>) stateField.get(check);
        if (states.containsKey(uuid)) return states.get(uuid);
        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.movement.InvalidPlaceCheck$PlacementState");
        java.lang.reflect.Constructor<?> ctor = stateClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object state = ctor.newInstance();
        states.put(uuid, state);
        return state;
    }

    @Test
    void constructor_readCheckData() {
        InvalidPlaceCheck check = createCheck();
        assertEquals("InvalidPlace A", check.getName());
        assertEquals("windfall.movement.invalidplace", check.getStableKey());
        assertEquals(20, check.getSetbackVl());
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        InvalidPlaceCheck check = createCheck();
        Field field = InvalidPlaceCheck.class.getDeclaredField("playerStates");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_placementsArePerPlayer() throws Exception {
        InvalidPlaceCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Object stateA = getOrCreateState(check, playerA.getUuid());
        Object stateB = getOrCreateState(check, playerB.getUuid());

        Field placementsField = stateA.getClass().getDeclaredField("placementsThisTick");
        placementsField.setAccessible(true);
        placementsField.setInt(stateA, 5);

        assertEquals(0, placementsField.getInt(stateB));
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        InvalidPlaceCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        getOrCreateState(check, playerA.getUuid());
        getOrCreateState(check, playerB.getUuid());

        Field stateField = InvalidPlaceCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, ?> states = (Map<UUID, ?>) stateField.get(check);
        assertEquals(2, states.size());
    }

    @Test
    void buffers_arePerPlayer() {
        InvalidPlaceCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 3.0);

        assertEquals(3.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
