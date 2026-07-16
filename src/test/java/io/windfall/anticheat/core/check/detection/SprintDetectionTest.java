package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.packet.SprintCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SprintDetectionTest extends CheckTestBase {

    private SprintCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new SprintCheck();
        player = createMockPlayer("TestPlayer");
    }

    private ClientCommandC2SPacket createSprintPacket(ClientCommandC2SPacket.Mode mode) throws Exception {
        ClientCommandC2SPacket packet = (ClientCommandC2SPacket) getUnsafe().allocateInstance(ClientCommandC2SPacket.class);
        Field modeField = ClientCommandC2SPacket.class.getDeclaredField("mode");
        modeField.setAccessible(true);
        modeField.set(packet, mode);
        return packet;
    }

    @Test
    void startSprint_noFlag() throws Exception {
        check.onPacketReceive(player, createSprintPacket(ClientCommandC2SPacket.Mode.START_SPRINTING));
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void doubleStartSprint_flags() throws Exception {
        check.onPacketReceive(player, createSprintPacket(ClientCommandC2SPacket.Mode.START_SPRINTING));
        check.onPacketReceive(player, createSprintPacket(ClientCommandC2SPacket.Mode.START_SPRINTING));
        assertTrue(check.getBuffer(player) > 0.0);
    }

    @Test
    void stopSprintWhileNotSprinting_flags() throws Exception {
        check.onPacketReceive(player, createSprintPacket(ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        assertTrue(check.getBuffer(player) > 0.0);
    }

    @Test
    void sprintWhileSneaking_increasesBufferFast() throws Exception {
        player.setSneaking(true);
        check.onPacketReceive(player, createSprintPacket(ClientCommandC2SPacket.Mode.START_SPRINTING));
        assertTrue(check.getBuffer(player) >= 2.0);
    }

    @Test
    void excessiveTogglesPerTick_flags() throws Exception {
        for (int i = 0; i < 5; i++) {
            check.onPacketReceive(player, createSprintPacket(ClientCommandC2SPacket.Mode.START_SPRINTING));
            check.onPacketReceive(player, createSprintPacket(ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        check.onTick(player);
        assertTrue(check.getBuffer(player) > 0.0 || check.getViolationLevel(player) > 0);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        check.onPacketReceive(player, createSprintPacket(ClientCommandC2SPacket.Mode.START_SPRINTING));
        UUID uuid = player.getUuid();
        Field stateField = SprintCheck.class.getDeclaredField("playerStates");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
