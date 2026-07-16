package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.ReachCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReachDetectionTest extends CheckTestBase {

    private ReachCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new ReachCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void movementPacket_recordsSnapshot() throws Exception {
        player.setPosition(0, 64, 0);
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));

        UUID uuid = player.getUuid();
        Field stateField = ReachCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));
    }

    @Test
    void attackWithoutServerPlayer_noFlag() throws Exception {
        player.setPosition(0, 64, 0);
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));
        check.onPacketReceive(player, createAttackPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setPosition(0, 64, 0);
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));

        UUID uuid = player.getUuid();
        Field stateField = ReachCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
