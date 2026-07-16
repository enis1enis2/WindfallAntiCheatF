package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.inventory.InventoryCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryDetectionTest extends CheckTestBase {

    private InventoryCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new InventoryCheck();
        player = createMockPlayer("TestPlayer");
    }

    private ClickSlotC2SPacket createClickPacket() throws Exception {
        return (ClickSlotC2SPacket) getUnsafe().allocateInstance(ClickSlotC2SPacket.class);
    }

    @Test
    void normalClick_noFlag() throws Exception {
        check.onPacketReceive(player, createClickPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void rapidClicks_flags() throws Exception {
        for (int i = 0; i < 10; i++) {
            check.onPacketReceive(player, createClickPacket());
        }
        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void slowClicks_noFlag() throws Exception {
        for (int i = 0; i < 5; i++) {
            check.onPacketReceive(player, createClickPacket());
            try { Thread.sleep(60); } catch (InterruptedException ignored) {}
        }
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void nonClickPacket_ignored() throws Exception {
        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        check.onPacketReceive(player, createClickPacket());
        UUID uuid = player.getUuid();
        Field stateField = InventoryCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
