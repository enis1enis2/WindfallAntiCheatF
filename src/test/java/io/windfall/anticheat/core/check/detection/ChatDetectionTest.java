package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.ChatCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class ChatDetectionTest extends CheckTestBase {

    private ChatCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new ChatCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void normalChat_noFlag() {
        check.onPacketReceive(player, createChatPacket("hello"));
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void burstDetection_exceedsThreshold_increasesBuffer() {
        for (int i = 0; i < 6; i++) {
            check.onPacketReceive(player, createChatPacket("msg" + i));
        }
        assertTrue(check.getBuffer(player) > 0.0);
    }

    @Test
    void burstDetection_exactlyAtThreshold_noFlag() {
        for (int i = 0; i < 4; i++) {
            check.onPacketReceive(player, createChatPacket("msg" + i));
        }
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void burstDetection_bufferAccumulatesOverThreshold() {
        for (int i = 0; i < 8; i++) {
            check.onPacketReceive(player, createChatPacket("msg" + i));
        }
        assertTrue(check.getBuffer(player) > 0.0);
    }

    @Test
    void nonChatPacket_ignored() {
        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void multiplePlayers_independentState() {
        WindfallPlayer player2 = createMockPlayer("Other");
        for (int i = 0; i < 6; i++) {
            check.onPacketReceive(player, createChatPacket("msg" + i));
        }
        assertTrue(check.getBuffer(player) > 0.0);
        assertEquals(0.0, check.getBuffer(player2), 0.001);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        check.onPacketReceive(player, createChatPacket("hello"));
        UUID uuid = player.getUuid();
        Field stateField = ChatCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
