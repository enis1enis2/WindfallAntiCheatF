package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.CrashCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CrashDetectionTest extends CheckTestBase {

    private CrashCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new CrashCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void normalChat_noFlag() {
        check.onPacketReceive(player, createChatPacket("hello"));
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void oversizedChat_increasesCount() throws Exception {
        String oversized = "x".repeat(32768);
        check.onPacketReceive(player, createChatPacket(oversized));

        Field stateField = CrashCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, ?> states = (Map<UUID, ?>) stateField.get(check);
        Object state = states.get(player.getUuid());
        Field countField = state.getClass().getDeclaredField("oversizedChatCount");
        countField.setAccessible(true);
        assertEquals(1, countField.getInt(state));
    }

    @Test
    void oversizedChat_threeTimes_flags() {
        String oversized = "x".repeat(32768);
        for (int i = 0; i < 3; i++) {
            check.onPacketReceive(player, createChatPacket(oversized));
        }
        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void oversizedChat_twoTimes_noFlag() {
        String oversized = "x".repeat(32768);
        check.onPacketReceive(player, createChatPacket(oversized));
        check.onPacketReceive(player, createChatPacket(oversized));
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void creativeBufferAboveThreshold_flags() {
        for (int i = 0; i < 22; i++) {
            check.increaseBuffer(player, 0.5);
        }
        assertTrue(check.getBuffer(player) > 10.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void nonChatPacket_ignored() {
        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        check.onPacketReceive(player, createChatPacket("hello"));
        UUID uuid = player.getUuid();
        Field stateField = CrashCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
