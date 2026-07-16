package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.PacketOrderCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PacketOrderDetectionTest extends CheckTestBase {

    private PacketOrderCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new PacketOrderCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void attackWithMovement_noFlag() {
        check.onPacketReceive(player, createMovePacket(0.0, 64.0, 0.0, true));
        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void consecutiveAttacksWithoutMovement_flags() {
        for (int i = 0; i < 6; i++) {
            check.onPacketReceive(player, createSwingPacket());
        }
        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void singleSwing_noFlag() {
        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void movementBetweenAttacks_noFlag() {
        check.onPacketReceive(player, createSwingPacket());
        check.onPacketReceive(player, createMovePacket(1.0, 64.0, 0.0, true));
        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void nonTrackedPacket_ignored() {
        check.onPacketReceive(player, createChatPacket("hello"));
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        check.onPacketReceive(player, createSwingPacket());
        UUID uuid = player.getUuid();
        Field stateField = PacketOrderCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
