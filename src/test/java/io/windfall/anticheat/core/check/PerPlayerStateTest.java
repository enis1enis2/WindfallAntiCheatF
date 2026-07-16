package io.windfall.anticheat.core.check;

import io.windfall.anticheat.core.check.impl.movement.VelocityCheck;
import io.windfall.anticheat.core.check.impl.movement.NoSwingCheck;
import io.windfall.anticheat.core.check.impl.inventory.InventoryCheck;
import io.windfall.anticheat.core.check.impl.packet.ChatCheck;
import io.windfall.anticheat.core.check.impl.packet.CrashCheck;
import io.windfall.anticheat.core.check.impl.packet.TransactionCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class PerPlayerStateTest extends CheckTestBase {

    @Test
    void velocityCheck_stateMap_isConcurrentHashMap() throws Exception {
        VelocityCheck check = new VelocityCheck();
        Field field = VelocityCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        assertInstanceOf(ConcurrentHashMap.class, field.get(check));
    }

    @Test
    void velocityCheck_perPlayerState_independent() throws Exception {
        VelocityCheck check = new VelocityCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");
        Method getStateMethod = VelocityCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);
        assertNotSame(stateA, stateB);
    }

    @Test
    void velocityCheck_removePlayer_clearsState() throws Exception {
        VelocityCheck check = new VelocityCheck();
        UUID uuid = UUID.randomUUID();
        WindfallPlayer player = createMockPlayer("Test", uuid);
        Method getStateMethod = VelocityCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, player);
        check.removePlayer(uuid);
        Field field = VelocityCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) field.get(check);
        assertFalse(map.containsKey(uuid));
    }

    @Test
    void noSwingCheck_stateMap_isConcurrentHashMap() throws Exception {
        NoSwingCheck check = new NoSwingCheck();
        Field field = NoSwingCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        assertInstanceOf(ConcurrentHashMap.class, field.get(check));
    }

    @Test
    void noSwingCheck_perPlayerState_independent() throws Exception {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");
        Method getStateMethod = NoSwingCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);
        assertNotSame(stateA, stateB);
    }

    @Test
    void inventoryCheck_stateMap_isConcurrentHashMap() throws Exception {
        InventoryCheck check = new InventoryCheck();
        Field field = InventoryCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        assertInstanceOf(ConcurrentHashMap.class, field.get(check));
    }

    @Test
    void inventoryCheck_perPlayerState_independent() throws Exception {
        InventoryCheck check = new InventoryCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");
        Method getStateMethod = InventoryCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);
        assertNotSame(stateA, stateB);
    }

    @Test
    void chatCheck_stateMap_isConcurrentHashMap() throws Exception {
        ChatCheck check = new ChatCheck();
        Field field = ChatCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        assertInstanceOf(ConcurrentHashMap.class, field.get(check));
    }

    @Test
    void chatCheck_perPlayerState_independent() throws Exception {
        ChatCheck check = new ChatCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");
        Method getStateMethod = ChatCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        Object stateA = getStateMethod.invoke(check, playerA);
        Object stateB = getStateMethod.invoke(check, playerB);
        assertNotSame(stateA, stateB);
    }

    @Test
    void crashCheck_stateMap_isConcurrentHashMap() throws Exception {
        CrashCheck check = new CrashCheck();
        Field field = CrashCheck.class.getDeclaredField("playerStates");
        field.setAccessible(true);
        assertInstanceOf(ConcurrentHashMap.class, field.get(check));
    }

    @Test
    void transactionCheck_stateMap_isConcurrentHashMap() throws Exception {
        TransactionCheck check = new TransactionCheck();
        Field field = TransactionCheck.class.getDeclaredField("stateMap");
        field.setAccessible(true);
        assertInstanceOf(ConcurrentHashMap.class, field.get(check));
    }

    @Test
    void buffers_arePerPlayer_acrossChecks() {
        VelocityCheck velCheck = new VelocityCheck();
        NoSwingCheck swingCheck = new NoSwingCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");
        velCheck.increaseBuffer(playerA, 2.5);
        swingCheck.increaseBuffer(playerA, 1.5);
        assertEquals(2.5, velCheck.getBuffer(playerA), 0.001);
        assertEquals(0.0, velCheck.getBuffer(playerB), 0.001);
        assertEquals(1.5, swingCheck.getBuffer(playerA), 0.001);
        assertEquals(0.0, swingCheck.getBuffer(playerB), 0.001);
    }

    @Test
    void violationLevels_areSharedAcrossChecks() {
        VelocityCheck velCheck = new VelocityCheck();
        NoSwingCheck swingCheck = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        player.getViolationLevels().put("windfall.movement.velocity", 5);
        player.getViolationLevels().put("windfall.movement.noswing", 3);
        assertEquals(5, velCheck.getViolationLevel(player));
        assertEquals(3, swingCheck.getViolationLevel(player));
    }
}
