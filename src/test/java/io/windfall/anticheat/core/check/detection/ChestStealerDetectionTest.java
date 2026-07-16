package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.ChestStealerCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ChestStealerDetectionTest extends CheckTestBase {

    private ChestStealerCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new ChestStealerCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void normalClick_noFlag() throws Exception {
        sun.misc.Unsafe unsafe = getUnsafe();

        Class<?> openScreenClass = Class.forName(
                "net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket",
                false, getClass().getClassLoader());
        Object openPacket = unsafe.allocateInstance(openScreenClass);
        check.onPacketSend(player, openPacket);

        Class<?> clickClass = Class.forName(
                "net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket",
                false, getClass().getClassLoader());
        Object clickPacket = unsafe.allocateInstance(clickClass);
        check.onPacketReceive(player, clickPacket);

        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        sun.misc.Unsafe unsafe = getUnsafe();

        Class<?> openScreenClass = Class.forName(
                "net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket",
                false, getClass().getClassLoader());
        Object openPacket = unsafe.allocateInstance(openScreenClass);
        check.onPacketSend(player, openPacket);

        Class<?> clickClass = Class.forName(
                "net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket",
                false, getClass().getClassLoader());
        Object clickPacket = unsafe.allocateInstance(clickClass);
        check.onPacketReceive(player, clickPacket);

        UUID uuid = player.getUuid();
        Field stateField = ChestStealerCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
