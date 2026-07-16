package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.AimCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AimDetectionTest extends CheckTestBase {

    private AimCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new AimCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void smallRotation_noFlag() throws Exception {
        player.setPosition(0, 64, 0);
        player.setYaw(0);

        player.setPosition(0.1, 64, 0);
        player.setYaw(5.0f);
        check.onPacketReceive(player, createMovePacket(0.1, 64, 0, true));

        player.setPosition(0.2, 64, 0);
        player.setYaw(10.0f);
        check.onPacketReceive(player, createMovePacket(0.2, 64, 0, true));

        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void instantSnap_flags() throws Exception {
        player.setPosition(0, 64, 0);
        player.setYaw(0);

        player.setPosition(0.1, 64, 0);
        check.onPacketReceive(player, createMovePacket(0.1, 64, 0, true));

        player.setPosition(0.2, 64, 0);
        player.setYaw(100.0f);
        check.onPacketReceive(player, createMovePacket(0.2, 64, 0, true));

        player.setPosition(0.3, 64, 0);
        player.setYaw(200.0f);
        check.onPacketReceive(player, createMovePacket(0.3, 64, 0, true));

        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void consistentRotation_flags() throws Exception {
        player.setPosition(0, 64, 0);
        player.setYaw(0);

        for (int i = 0; i < 25; i++) {
            player.setPosition(i * 0.1, 64, 0);
            player.setYaw(i * 5.0f);
            check.onPacketReceive(player, createMovePacket(i * 0.1, 64, 0, true));
        }

        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setPosition(0, 64, 0);
        player.setYaw(0);
        player.setPosition(0.1, 64, 0);
        check.onPacketReceive(player, createMovePacket(0.1, 64, 0, true));

        UUID uuid = player.getUuid();
        Field stateField = AimCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
