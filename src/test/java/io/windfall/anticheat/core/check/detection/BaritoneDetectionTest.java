package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.BaritoneCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BaritoneDetectionTest extends CheckTestBase {

    private BaritoneCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new BaritoneCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void variedMovement_noFlag() {
        player.setPosition(0, 64, 0);
        player.setYaw(0.0f);

        check.onPacketReceive(player, createFullMovePacket(0, 64, 0, 0.0f, 0, true));

        for (int i = 1; i <= 50; i++) {
            double speed = 0.1 + (i % 5) * 0.05;
            double x = i * speed;
            float yaw = i * 15.0f;
            player.setPosition(x, 64, 0);
            player.setYaw(yaw);
            check.onPacketReceive(player, createFullMovePacket(x, 64, 0, yaw, 0, true));
        }

        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setPosition(0, 64, 0);
        player.setYaw(0.0f);
        check.onPacketReceive(player, createFullMovePacket(0, 64, 0, 0.0f, 0, true));

        UUID uuid = player.getUuid();
        Field stateField = BaritoneCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
