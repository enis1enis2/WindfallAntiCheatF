package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.movement.ElytraCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ElytraDetectionTest extends CheckTestBase {

    private ElytraCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new ElytraCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void normalGliding_noFlag() {
        player.setGliding(true);
        player.setPosition(0, 64, 0);

        for (int i = 0; i < 20; i++) {
            double x = (i + 1) * 0.3;
            double y = 64 - (i + 1) * 0.05;
            player.setPosition(x, y, 0);
            check.onPacketReceive(player, createMovePacket(x, y, 0, false));
        }

        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void highHorizontalSpeedWhileGliding_flags() {
        player.setGliding(true);
        player.setPosition(0, 64, 0);

        for (int i = 0; i < 8; i++) {
            double x = (i + 1) * 4.0;
            player.setPosition(x, 64, 0);
            check.onPacketReceive(player, createMovePacket(x, 64, 0, false));
        }

        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setGliding(true);
        player.setPosition(0, 64, 0);
        player.setPosition(2, 64, 0);
        check.onPacketReceive(player, createMovePacket(2, 64, 0, false));

        UUID uuid = player.getUuid();
        Field stateField = ElytraCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
