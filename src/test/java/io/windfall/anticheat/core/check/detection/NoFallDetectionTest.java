package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.NoFallCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NoFallDetectionTest extends CheckTestBase {

    private NoFallCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new NoFallCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void onGroundWithNoFallDistance_noFlag() {
        player.setPosition(0, 64, 0);
        player.setOnGround(true);
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void fallingAndClaimingGround_flagsAfterConsecutive() {
        player.setPosition(0, 100, 0);
        player.setOnGround(true);

        for (int tick = 0; tick < 6; tick++) {
            double y = 100 - (tick + 1) * 4;
            player.setOnGround(true);
            player.setPosition(0, y, 0);
            check.onPacketReceive(player, createMovePacket(0, y, 0, true));
        }

        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void sustainedFlight_noFlag() {
        player.setPosition(0, 100, 0);
        player.setOnGround(false);
        player.setFlying(true);

        for (int tick = 0; tick < 10; tick++) {
            double y = 100 - tick;
            check.onPacketReceive(player, createMovePacket(0, y, 0, false));
            player.setPosition(0, y, 0);
        }

        player.setFlying(false);
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void gradualDescent_noFlag() {
        player.setPosition(0, 64, 0);
        player.setOnGround(true);
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));
        player.setPosition(0, 64, 0);

        for (int tick = 0; tick < 5; tick++) {
            double y = 63.9;
            check.onPacketReceive(player, createMovePacket(0, y, 0, true));
            player.setPosition(0, y, 0);
        }

        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setPosition(0, 100, 0);
        player.setOnGround(false);
        player.setPosition(0, 96, 0);
        check.onPacketReceive(player, createMovePacket(0, 96, 0, true));

        UUID uuid = player.getUuid();
        Field stateField = NoFallCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
