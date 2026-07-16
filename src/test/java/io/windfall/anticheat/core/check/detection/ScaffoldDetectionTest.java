package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.ScaffoldCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScaffoldDetectionTest extends CheckTestBase {

    private ScaffoldCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new ScaffoldCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void normalMovement_noFlag() {
        player.setPosition(0, 64, 0);
        player.setOnGround(true);

        for (int i = 0; i < 15; i++) {
            double x = i * 0.2;
            player.setPosition(x, 64, 0);
            player.setOnGround(true);
            check.onPacketReceive(player, createMovePacket(x, 64, 0, true));
            check.onTick(player, 0L);
        }

        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setPosition(0, 64, 0);
        player.setOnGround(true);
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));
        check.onTick(player, 0L);

        UUID uuid = player.getUuid();
        Field stateField = ScaffoldCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid.toString()));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid.toString()));
    }
}
