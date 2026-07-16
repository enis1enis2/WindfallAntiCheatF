package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.MacroCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MacroDetectionTest extends CheckTestBase {

    private MacroCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new MacroCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void variedMovement_noFlag() {
        double[][] movements = {
            {0.1, 0, -0.1},
            {-0.1, 0, 0.1},
            {0.0, 0, -0.2},
            {0.2, 0, 0.0},
            {-0.15, 0, -0.15},
            {0.15, 0, 0.15},
            {0.0, 0, 0.1},
            {-0.2, 0, 0.0},
            {0.1, 0, 0.1},
            {-0.1, 0, -0.1},
            {0.2, 0, -0.2},
            {-0.2, 0, 0.2},
            {0.05, 0, -0.15},
            {-0.05, 0, 0.15},
            {0.15, 0, 0.05},
            {-0.15, 0, -0.05},
            {0.1, 0, 0.2},
            {-0.1, 0, -0.2},
            {0.2, 0, 0.1},
            {-0.2, 0, -0.1},
            {0.0, 0, -0.1},
            {0.0, 0, 0.1},
            {0.1, 0, 0.0},
            {-0.1, 0, 0.0},
        };

        for (double[] m : movements) {
            player.setPosition(player.getX() + m[0], player.getY() + m[1], player.getZ() + m[2]);
            check.onPacketReceive(player, createMovePacket(player.getX(), player.getY(), player.getZ(), true));
        }

        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void singleMovePacket_noFlag() {
        player.setPosition(0.5, 64.0, -0.5);
        check.onPacketReceive(player, createMovePacket(0.5, 64.0, -0.5, true));
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void nonMovePacket_ignored() {
        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setPosition(0.5, 64.0, -0.5);
        check.onPacketReceive(player, createMovePacket(0.5, 64.0, -0.5, true));
        UUID uuid = player.getUuid();
        Field stateField = MacroCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
