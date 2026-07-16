package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.CriticalsCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CriticalsDetectionTest extends CheckTestBase {

    private CriticalsCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new CriticalsCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void attackOnGround_noFlag() throws Exception {
        player.setPosition(0, 64, 0);
        player.setOnGround(true);

        check.onPacketReceive(player, createAttackPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void attackWhileFlying_noFlag() throws Exception {
        player.setPosition(0, 100, 0);
        player.setOnGround(false);
        player.setFlying(true);

        check.onPacketReceive(player, createAttackPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void attackWhileGliding_noFlag() throws Exception {
        player.setPosition(0, 100, 0);
        player.setOnGround(false);
        player.setGliding(true);

        check.onPacketReceive(player, createAttackPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void attackWhileSneaking_noFlag() throws Exception {
        player.setPosition(0, 64, 0);
        player.setOnGround(false);
        player.setSneaking(true);

        check.onPacketReceive(player, createAttackPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void attackAirborneNoMotion_buffersIncrease() throws Exception {
        player.setPosition(0, 100, 0);
        player.setOnGround(false);

        for (int i = 0; i < 5; i++) {
            player.setPosition(0, 100, 0);
            check.onPacketReceive(player, createAttackPacket());
        }
        assertTrue(check.getBuffer(player) > 0.0);
    }

    @Test
    void attackAirborneNoMotion_fourConsecutive_flagsWithSetback() throws Exception {
        player.setPosition(0, 100, 0);
        player.setOnGround(false);

        for (int i = 0; i < 4; i++) {
            player.setPosition(0, 100, 0);
            player.setOnGround(false);
            check.onPacketReceive(player, createAttackPacket());
        }
        assertTrue(check.getViolationLevel(player) > 0);
    }

    @Test
    void attackAirborneWithValidMotion_noFlag() throws Exception {
        player.setPosition(0, 100, 0);
        player.setOnGround(false);
        player.setVelocityY(0.2);

        check.onPacketReceive(player, createAttackPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void nonAttackPacket_ignored() throws Exception {
        player.setPosition(0, 100, 0);
        player.setOnGround(false);

        check.onPacketReceive(player, createSwingPacket());
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        player.setPosition(0, 100, 0);
        player.setOnGround(false);
        player.setPosition(0, 100, 0);
        check.onPacketReceive(player, createAttackPacket());

        UUID uuid = player.getUuid();
        Field stateField = CriticalsCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> states = (Map<?, ?>) stateField.get(check);
        assertTrue(states.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(states.containsKey(uuid));
    }
}
