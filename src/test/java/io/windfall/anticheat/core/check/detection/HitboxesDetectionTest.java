package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.HitboxesCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HitboxesDetectionTest extends CheckTestBase {

    private HitboxesCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new HitboxesCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void fewAttacks_noFlag() throws Exception {
        player.setPosition(0, 64, 0);
        player.setYaw(0);
        player.setPitch(0);

        for (int i = 0; i < 15; i++) {
            check.onPacketReceive(player, createAttackPacket());
        }
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setPosition(0, 64, 0);
        player.setYaw(0);
        player.setPitch(0);
        check.onPacketReceive(player, createAttackPacket());

        UUID uuid = player.getUuid();
        Field stateField = HitboxesCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
