package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.MultiInteractCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MultiInteractDetectionTest extends CheckTestBase {

    private MultiInteractCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new MultiInteractCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void singleAttack_noFlag() throws Exception {
        check.onPacketReceive(player, createAttackPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        UUID uuid = player.getUuid();

        Field stateMapField = MultiInteractCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> stateMap = (Map<UUID, Object>) stateMapField.get(check);

        Class<?> interactStateClass = Class.forName("io.windfall.anticheat.core.check.impl.combat.MultiInteractCheck$InteractState");
        Object interactState = getUnsafe().allocateInstance(interactStateClass);
        stateMap.put(uuid, interactState);

        assertTrue(stateMap.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(stateMap.containsKey(uuid));
    }
}
