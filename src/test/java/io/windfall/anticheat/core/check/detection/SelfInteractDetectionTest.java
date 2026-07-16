package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.SelfInteractCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SelfInteractDetectionTest extends CheckTestBase {

    private SelfInteractCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new SelfInteractCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void nonAttackPacket_noFlag() throws Exception {
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void swingPacket_noFlag() throws Exception {
        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void chatPacket_noFlag() throws Exception {
        check.onPacketReceive(player, createChatPacket("hello"));
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_noError() throws Exception {
        assertDoesNotThrow(() -> check.removePlayer(player.getUuid()));
    }
}
