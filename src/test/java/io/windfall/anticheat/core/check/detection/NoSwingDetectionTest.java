package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.NoSwingCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class NoSwingDetectionTest extends CheckTestBase {

    private NoSwingCheck createCheck() { return new NoSwingCheck(); }

    @Test
    void constructor_readCheckData() {
        NoSwingCheck check = createCheck();
        assertEquals("No Swing A", check.getName());
        assertEquals("windfall.movement.noswing", check.getStableKey());
        assertEquals(10, check.getSetbackVl());
        assertEquals(0.02, check.getDecay(), 0.001);
    }

    @Test
    void stateMap_isConcurrentHashMap() throws Exception {
        NoSwingCheck check = createCheck();
        Field field = NoSwingCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Object stateMap = field.get(check);
        assertInstanceOf(ConcurrentHashMap.class, stateMap);
    }

    @Test
    void perPlayerState_differentPlayersGetDifferentState() throws Exception {
        NoSwingCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        Field stateMapField = NoSwingCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);

        java.lang.reflect.Method getStateMethod = NoSwingCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, playerA);
        getStateMethod.invoke(check, playerB);

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<UUID, ?> stateMap = (ConcurrentHashMap<UUID, ?>) stateMapField.get(check);
        assertEquals(2, stateMap.size());
        assertNotEquals(
            stateMap.get(playerA.getUuid()),
            stateMap.get(playerB.getUuid())
        );
    }

    @Test
    void perPlayerState_playerAStateDoesNotAffectPlayerB() throws Exception {
        NoSwingCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        java.lang.reflect.Method getStateMethod = NoSwingCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);

        Field lastSwingField = stateA.getClass().getDeclaredField("lastSwingTime");
        lastSwingField.setAccessible(true);
        lastSwingField.setLong(stateA, 99999L);

        assertEquals(0L, lastSwingField.getLong(stateB));
    }

    @Test
    void buffers_arePerPlayer() {
        NoSwingCheck check = createCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");

        check.increaseBuffer(playerA, 3.0);

        assertEquals(3.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }
}
