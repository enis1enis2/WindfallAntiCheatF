package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.BacktrackCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BacktrackDetectionTest extends CheckTestBase {

    private BacktrackCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new BacktrackCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void attackAfterRecentMovement_noFlag() throws Exception {
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));
        Thread.sleep(50);
        check.onPacketReceive(player, createAttackPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void attackAfterLongDelay_flags() throws Exception {
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));
        Thread.sleep(600);
        check.onPacketReceive(player, createAttackPacket());
        assertTrue(check.getBuffer(player) > 0.0);
    }

    @Test
    void attackWithNoMovement_flags() throws Exception {
        check.onPacketReceive(player, createAttackPacket());
        assertTrue(check.getBuffer(player) > 0.0);
    }

    @Test
    void multipleAttacksWithMovement_noFlag() throws Exception {
        for (int i = 0; i < 10; i++) {
            check.onPacketReceive(player, createMovePacket(i, 64, 0, true));
            check.onPacketReceive(player, createAttackPacket());
        }
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        check.onPacketReceive(player, createMovePacket(0, 64, 0, true));
        UUID uuid = player.getUuid();
        Field stateField = BacktrackCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
