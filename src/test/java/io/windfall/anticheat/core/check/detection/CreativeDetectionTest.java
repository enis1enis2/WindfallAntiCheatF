package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.CreativeCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class CreativeDetectionTest extends CheckTestBase {

    private CreativeCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new CreativeCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void creativeActionsPerTick_exceedsThreshold_increasesBuffer() throws Exception {
        Field stateField = CreativeCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> states = (Map<UUID, Object>) stateField.get(check);

        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.packet.CreativeCheck$PlayerState");
        Object state = getUnsafe().allocateInstance(stateClass);
        Field countField = stateClass.getDeclaredField("creativeActionsThisTick");
        countField.setAccessible(true);
        countField.setInt(state, 7);
        states.put(player.getUuid(), state);

        assertTrue(countField.getInt(state) > 5);
    }

    @Test
    void creativeActionsPerTick_resetsOnTick() throws Exception {
        Field stateField = CreativeCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> states = (Map<UUID, Object>) stateField.get(check);

        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.packet.CreativeCheck$PlayerState");
        Object state = getUnsafe().allocateInstance(stateClass);
        Field countField = stateClass.getDeclaredField("creativeActionsThisTick");
        countField.setAccessible(true);
        countField.setInt(state, 6);
        states.put(player.getUuid(), state);

        check.onTick(player);

        assertEquals(0, countField.getInt(state));
    }

    @Test
    void nonCreativePacket_ignored() {
        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        Field stateField = CreativeCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> states = (Map<UUID, Object>) stateField.get(check);

        Class<?> stateClass = Class.forName("io.windfall.anticheat.core.check.impl.packet.CreativeCheck$PlayerState");
        Object state = getUnsafe().allocateInstance(stateClass);
        states.put(player.getUuid(), state);
        assertTrue(states.containsKey(player.getUuid()));

        check.removePlayer(player.getUuid());
        assertFalse(states.containsKey(player.getUuid()));
    }
}
