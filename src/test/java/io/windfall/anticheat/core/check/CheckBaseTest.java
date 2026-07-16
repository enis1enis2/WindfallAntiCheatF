package io.windfall.anticheat.core.check;

import io.windfall.anticheat.core.check.impl.combat.*;
import io.windfall.anticheat.core.check.impl.movement.*;
import io.windfall.anticheat.core.check.impl.packet.*;
import io.windfall.anticheat.core.check.impl.inventory.InventoryCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class CheckBaseTest extends CheckTestBase {

    @Test
    void criticalsCheck_metadata() {
        CriticalsCheck check = new CriticalsCheck();
        assertEquals("Criticals A", check.getName());
        assertEquals("windfall.combat.criticals", check.getStableKey());
        assertEquals(10, check.getSetbackVl());
    }

    @Test
    void selfInteractCheck_metadata() {
        SelfInteractCheck check = new SelfInteractCheck();
        assertEquals("Self Interact A", check.getName());
        assertEquals("windfall.combat.selfinteract", check.getStableKey());
    }

    @Test
    void noSwingCheck_metadata() {
        NoSwingCheck check = new NoSwingCheck();
        assertEquals("No Swing A", check.getName());
        assertEquals("windfall.movement.noswing", check.getStableKey());
        assertEquals(10, check.getSetbackVl());
    }

    @Test
    void velocityCheck_metadata() {
        VelocityCheck check = new VelocityCheck();
        assertEquals("Velocity A", check.getName());
        assertEquals("windfall.movement.velocity", check.getStableKey());
        assertEquals(30, check.getSetbackVl());
    }

    @Test
    void inventoryCheck_metadata() {
        InventoryCheck check = new InventoryCheck();
        assertEquals("Inventory A", check.getName());
        assertEquals("windfall.inventory.inventory", check.getStableKey());
        assertEquals(15, check.getSetbackVl());
    }

    @Test
    void chatCheck_metadata() {
        ChatCheck check = new ChatCheck();
        assertEquals("Chat A", check.getName());
        assertEquals("windfall.packet.chat", check.getStableKey());
    }

    @Test
    void transactionCheck_metadata() {
        TransactionCheck check = new TransactionCheck();
        assertEquals("Transaction A", check.getName());
        assertEquals("windfall.packet.transaction", check.getStableKey());
    }

    @Test
    void increaseBuffer_accumulates() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        check.increaseBuffer(player, 1.0);
        check.increaseBuffer(player, 1.5);
        assertEquals(2.5, check.getBuffer(player), 0.001);
    }

    @Test
    void decreaseBuffer_clampsAtZero() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        check.increaseBuffer(player, 1.0);
        check.decreaseBuffer(player, 5.0);
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void resetBuffer_clearsBuffer() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        check.increaseBuffer(player, 5.0);
        check.resetBuffer(player);
        assertEquals(0.0, check.getBuffer(player), 0.001);
    }

    @Test
    void separatePlayers_haveIndependentBuffers() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");
        check.increaseBuffer(playerA, 5.0);
        assertEquals(5.0, check.getBuffer(playerA), 0.001);
        assertEquals(0.0, check.getBuffer(playerB), 0.001);
    }

    @Test
    void separatePlayers_haveIndependentViolationLevels() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer playerA = createMockPlayer("Alice");
        WindfallPlayer playerB = createMockPlayer("Bob");
        playerA.getViolationLevels().put("windfall.movement.noswing", 5);
        assertEquals(5, check.getViolationLevel(playerA));
        assertEquals(0, check.getViolationLevel(playerB));
    }

    @Test
    void flag_incrementsViolationLevel() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        check.flag(player);
        assertEquals(1, check.getViolationLevel(player));
    }

    @Test
    void flag_doesNothingWhenDisabled() throws Exception {
        setConfigValue("checks.windfall.movement.noswing.enabled", false);
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        check.flag(player);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void reward_decaysBuffer() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        check.increaseBuffer(player, 1.0);
        check.reward(player);
        double decay = check.getDecay();
        double expected = Math.max(0.0, 1.0 - decay);
        assertEquals(expected, check.getBuffer(player), 0.001);
    }

    @Test
    void reward_decaysViolationLevel() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        player.getViolationLevels().put("windfall.movement.noswing", 5);
        check.reward(player);
        assertEquals(4, check.getViolationLevel(player));
    }

    @Test
    void reward_vlOne_resetsToZero() {
        NoSwingCheck check = new NoSwingCheck();
        WindfallPlayer player = createMockPlayer("Test");
        player.getViolationLevels().put("windfall.movement.noswing", 1);
        check.reward(player);
        assertEquals(0, check.getViolationLevel(player));
    }

    @Test
    void all53Checks_instantiateWithoutError() {
        assertDoesNotThrow(() -> new AimCheck());
        assertDoesNotThrow(() -> new AutoclickerCheck());
        assertDoesNotThrow(() -> new BacktrackCheck());
        assertDoesNotThrow(() -> new CriticalsCheck());
        assertDoesNotThrow(() -> new FastHealCheck());
        assertDoesNotThrow(() -> new HitboxesCheck());
        assertDoesNotThrow(() -> new KillAuraCheck());
        assertDoesNotThrow(() -> new MacroCheck());
        assertDoesNotThrow(() -> new MultiInteractCheck());
        assertDoesNotThrow(() -> new ReachCheck());
        assertDoesNotThrow(() -> new SelfInteractCheck());
        assertDoesNotThrow(() -> new SwordBlockCheck());
        assertDoesNotThrow(() -> new SpeedCheck());
        assertDoesNotThrow(() -> new FlightCheck());
        assertDoesNotThrow(() -> new VelocityCheck());
        assertDoesNotThrow(() -> new TimerCheck());
        assertDoesNotThrow(() -> new NoFallCheck());
        assertDoesNotThrow(() -> new StepCheck());
        assertDoesNotThrow(() -> new ScaffoldCheck());
        assertDoesNotThrow(() -> new ElytraCheck());
        assertDoesNotThrow(() -> new BaritoneCheck());
        assertDoesNotThrow(() -> new GroundSpoofCheck());
        assertDoesNotThrow(() -> new PhaseCheck());
        assertDoesNotThrow(() -> new SimulationCheck());
        assertDoesNotThrow(() -> new NoSlowCheck());
        assertDoesNotThrow(() -> new MotionCheck());
        assertDoesNotThrow(() -> new FastBreakCheck());
        assertDoesNotThrow(() -> new FarBreakCheck());
        assertDoesNotThrow(() -> new FarPlaceCheck());
        assertDoesNotThrow(() -> new InvalidBreakCheck());
        assertDoesNotThrow(() -> new InvalidPlaceCheck());
        assertDoesNotThrow(() -> new NoSwingCheck());
        assertDoesNotThrow(() -> new RotationBreakCheck());
        assertDoesNotThrow(() -> new AirLiquidBreakCheck());
        assertDoesNotThrow(() -> new WrongBreakCheck());
        assertDoesNotThrow(() -> new PositionBreakCheck());
        assertDoesNotThrow(() -> new MultiBreakCheck());
        assertDoesNotThrow(() -> new AirLiquidPlaceCheck());
        assertDoesNotThrow(() -> new RotationPlaceCheck());
        assertDoesNotThrow(() -> new PositionPlaceCheck());
        assertDoesNotThrow(() -> new MultiPlaceCheck());
        assertDoesNotThrow(() -> new BadPacketsCheck());
        assertDoesNotThrow(() -> new ChestStealerCheck());
        assertDoesNotThrow(() -> new CreativeCheck());
        assertDoesNotThrow(() -> new PacketOrderCheck());
        assertDoesNotThrow(() -> new ChatCheck());
        assertDoesNotThrow(() -> new CrashCheck());
        assertDoesNotThrow(() -> new SprintCheck());
        assertDoesNotThrow(() -> new ExploitCheck());
        assertDoesNotThrow(() -> new ClientBrandCheck());
        assertDoesNotThrow(() -> new VehicleCheck());
        assertDoesNotThrow(() -> new TransactionCheck());
        assertDoesNotThrow(() -> new InventoryCheck());
    }

    @Test
    void removePlayer_clearsInternalState() throws Exception {
        VelocityCheck check = new VelocityCheck();
        UUID uuid = UUID.randomUUID();
        WindfallPlayer player = createMockPlayer("Test", uuid);
        Method getStateMethod = VelocityCheck.class.getDeclaredMethod("getState", WindfallPlayer.class);
        getStateMethod.setAccessible(true);
        getStateMethod.invoke(check, player);
        Field stateField = VelocityCheck.class.getDeclaredField("stateMap");
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> map = (Map<?, ?>) stateField.get(check);
        assertTrue(map.containsKey(uuid));
        check.removePlayer(uuid);
        assertFalse(map.containsKey(uuid));
    }
}
