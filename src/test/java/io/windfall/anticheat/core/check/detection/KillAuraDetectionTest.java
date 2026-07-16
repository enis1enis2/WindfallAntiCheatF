package io.windfall.anticheat.core.check.detection;

import io.windfall.anticheat.core.check.CheckTestBase;
import io.windfall.anticheat.core.check.impl.combat.KillAuraCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KillAuraDetectionTest extends CheckTestBase {

    private KillAuraCheck check;
    private WindfallPlayer player;

    @BeforeEach
    void setUp() throws Exception {
        check = new KillAuraCheck();
        player = createMockPlayer("TestPlayer");
    }

    @Test
    void attackSingleTarget_noFlag() throws Exception {
        for (int i = 0; i < 10; i++) {
            check.onPacketReceive(player, createAttackPacket());
        }
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void multipleUniqueTargets_flags() throws Exception {
        Class<?> psClass = Class.forName("io.windfall.anticheat.core.check.impl.combat.KillAuraCheck$PlayerState");
        Object playerState = getUnsafe().allocateInstance(psClass);

        Field recentTargetsField = psClass.getDeclaredField("recentTargets");
        recentTargetsField.setAccessible(true);
        getUnsafe().putObject(playerState, getUnsafe().objectFieldOffset(recentTargetsField), new ArrayDeque<>());

        Field yawDeltasField = psClass.getDeclaredField("recentYawDeltas");
        yawDeltasField.setAccessible(true);
        getUnsafe().putObject(playerState, getUnsafe().objectFieldOffset(yawDeltasField), new ArrayDeque<>());

        Field totalAttacksField = psClass.getDeclaredField("totalAttacks");
        totalAttacksField.setAccessible(true);
        totalAttacksField.setInt(playerState, 19);

        @SuppressWarnings("unchecked")
        Deque<Object> recentTargets = (Deque<Object>) recentTargetsField.get(playerState);

        Class<?> teClass = Class.forName("io.windfall.anticheat.core.check.impl.combat.KillAuraCheck$TargetEvent");
        java.lang.reflect.Constructor<?> teCtor = teClass.getDeclaredConstructor(int.class, long.class);
        teCtor.setAccessible(true);

        long now = System.currentTimeMillis();
        for (int i = 1; i <= 5; i++) {
            recentTargets.addLast(teCtor.newInstance(i + 100, now));
        }

        Field stateMapField = KillAuraCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> stateMap = (Map<UUID, Object>) stateMapField.get(check);
        stateMap.put(player.getUuid(), playerState);

        check.onPacketReceive(player, createAttackPacket());
        assertTrue(check.getBuffer(player) > 0.0);
    }

    @Test
    void removePlayer_clearsState() throws Exception {
        Class<?> psClass = Class.forName("io.windfall.anticheat.core.check.impl.combat.KillAuraCheck$PlayerState");
        Object playerState = getUnsafe().allocateInstance(psClass);

        Field stateMapField = KillAuraCheck.class.getDeclaredField("stateMap");
        stateMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<UUID, Object> stateMap = (Map<UUID, Object>) stateMapField.get(check);
        stateMap.put(player.getUuid(), playerState);

        UUID uuid = player.getUuid();
        assertTrue(stateMap.containsKey(uuid));

        check.removePlayer(uuid);
        assertFalse(stateMap.containsKey(uuid));
    }
}
