package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.AutoclickerCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AutoclickerDetectionTest extends CheckTestBase {

    private AutoclickerCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new AutoclickerCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void normalClickRate_noFlag() throws Exception {
        for (int i = 0; i < 8; i++) {
            check.onPacketReceive(player, createAttackPacket());
            Thread.sleep(150);
        }
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        check.onPacketReceive(player, createAttackPacket());

        UUID uuid = player.getUuid();
        Field stateField = AutoclickerCheck.class.getDeclaredField("clickStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
