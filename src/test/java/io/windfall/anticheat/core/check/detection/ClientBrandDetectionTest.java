package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.ClientBrandCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ClientBrandDetectionTest extends CheckTestBase {

    private ClientBrandCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new ClientBrandCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void checkName_isCorrect() {
        assertEquals("ClientBrand A", check.getName());
    }

    @Test
    void stableKey_isCorrect() {
        assertEquals("windfall.packet.clientbrand", check.getStableKey());
    }

    @Test
    void nonBrandPacket_ignored() {
        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void chatPacket_ignored() {
        check.onPacketReceive(player, createChatPacket("wurst"));
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_noError() {
        UUID uuid = player.getUuid();
        assertDoesNotThrow(() -> check.removePlayer(uuid));
    }
}
