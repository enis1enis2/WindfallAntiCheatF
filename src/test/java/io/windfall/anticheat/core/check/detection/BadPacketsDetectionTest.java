package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.BadPacketsCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BadPacketsDetectionTest extends CheckTestBase {

    private BadPacketsCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new BadPacketsCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void nanCoordinates_flagsAndResetsBuffer() {
        check.onPacketReceive(player, createMovePacket(Double.NaN, 64.0, 0.0, true));
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void infiniteCoordinates_flagsAndResetsBuffer() {
        check.onPacketReceive(player, createMovePacket(Double.POSITIVE_INFINITY, 64.0, 0.0, true));
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void nanZ_flags() {
        check.onPacketReceive(player, createMovePacket(0.0, 64.0, Double.NaN, true));
        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void yOutOfBounds_aboveMax_flags() {
        check.onPacketReceive(player, createMovePacket(0.0, 401.0, 0.0, true));
        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void yOutOfBounds_belowMin_flags() {
        check.onPacketReceive(player, createMovePacket(0.0, -65.0, 0.0, true));
        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void yAtBoundary_noFlag() {
        check.onPacketReceive(player, createMovePacket(0.0, 400.0, 0.0, true));
        assertEquals(0, check.getViolationLevel(player));

        check.onPacketReceive(player, createMovePacket(0.0, -64.0, 0.0, true));
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void nanRotation_flags() {
        check.onPacketReceive(player, createFullMovePacket(0.0, 64.0, 0.0, Float.NaN, 0.0f, true));
        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void infiniteRotation_flags() {
        check.onPacketReceive(player, createFullMovePacket(0.0, 64.0, 0.0, 0.0f, Float.POSITIVE_INFINITY, true));
        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void rotationOutOfRange_yawTooHigh_increasesBuffer() {
        for (int i = 0; i < 3; i++) {
            check.onPacketReceive(player, createFullMovePacket(0.0, 64.0, 0.0, 181.0f, 0.0f, true));
        }
        assertTrue(check.getBuffer(player) > 2.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void rotationOutOfRange_pitchTooLow_increasesBuffer() {
        for (int i = 0; i < 3; i++) {
            check.onPacketReceive(player, createFullMovePacket(0.0, 64.0, 0.0, 0.0f, -91.0f, true));
        }
        assertTrue(check.getBuffer(player) > 2.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void rotationInRange_noFlag() {
        check.onPacketReceive(player, createFullMovePacket(0.0, 64.0, 0.0, 90.0f, 45.0f, true));
        check.onPacketReceive(player, createFullMovePacket(0.0, 64.0, 0.0, -90.0f, -45.0f, true));
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void duplicatePacketDetection_increasesBuffer() {
        for (int i = 0; i < 11; i++) {
            PlayerMoveC2SPacket pkt = createMovePacket(i, 64.0, 0.0, true);
            check.onPacketReceive(player, pkt);
        }
        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void autoClickerDetection_flags() {
        for (int i = 0; i < 22; i++) {
            check.onPacketReceive(player, createSwingPacket());
        }
        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void autoClickerDetection_slowClicks_noFlag() {
        for (int i = 0; i < 15; i++) {
            check.onPacketReceive(player, createSwingPacket());
            check.onPacketReceive(player, createMovePacket(i, 64.0, 0.0, true));
            try { Thread.sleep(5); } catch (InterruptedException ignored) {}
        }
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void validPacket_noFlag() {
        check.onPacketReceive(player, createMovePacket(0.0, 64.0, 0.0, true));
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        check.onPacketReceive(player, createSwingPacket());
        UUID uuid = player.getUuid();
        Field stateField = BadPacketsCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
