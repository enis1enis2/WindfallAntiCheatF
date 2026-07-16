package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.SimulationCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SimulationDetectionTest extends CheckTestBase {

    private static final double GRAVITY = 0.08;
    private static final double AIR_DRAG = 0.98;

    private SimulationCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new SimulationCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void normalPhysics_noFlag() {
        player.setPosition(0, 64, 0);
        player.setOnGround(true);
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));

        double lastDeltaY = 0;
        double y = 64;

        for (int tick = 0; tick < 10; tick++) {
            double predictedDeltaY = (lastDeltaY - GRAVITY) * AIR_DRAG;
            y += predictedDeltaY;
            player.setOnGround(false);
            player.setPosition(0, y, 0);
            check.onPacketReceive(player, createMovePacket(0, y, 0, false));
            lastDeltaY = predictedDeltaY;
        }

        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void impossiblePosition_flags() {
        player.setPosition(0, 64, 0);
        player.setOnGround(true);
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));

        double jumpVel = 0.42;
        player.setOnGround(false);
        double y1 = 64 + jumpVel;
        player.setPosition(0, y1, 0);
        check.onPacketReceive(player, createMovePacket(0, y1, 0, false));

        double deltaY2 = (jumpVel - GRAVITY) * AIR_DRAG;
        double y2 = y1 + deltaY2;
        player.setPosition(0, y2, 0);
        check.onPacketReceive(player, createMovePacket(0, y2, 0, false));

        double impossibleY = y2 + 10;
        player.setPosition(0, impossibleY, 0);
        check.onPacketReceive(player, createMovePacket(0, impossibleY, 0, false));

        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void sustainedFlight_noFlag() {
        player.setPosition(0, 64, 0);
        player.setOnGround(false);
        player.setFlying(true);

        for (int i = 0; i < 10; i++) {
            double y = 64 + i * 0.5;
            player.setPosition(0, y, 0);
            check.onPacketReceive(player, createMovePacket(0, y, 0, false));
        }

        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setPosition(0, 64, 0);
        player.setOnGround(true);
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));

        UUID uuid = player.getUuid();
        Field stateField = SimulationCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid.toString()));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid.toString()));
    }
}
