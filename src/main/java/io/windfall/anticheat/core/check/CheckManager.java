package io.windfall.anticheat.core.check;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.impl.combat.*;
import io.windfall.anticheat.core.check.impl.movement.*;
import io.windfall.anticheat.core.check.impl.packet.*;
import io.windfall.anticheat.core.check.impl.inventory.InventoryCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.adaptive.AdaptiveThreshold;
import io.windfall.anticheat.core.compensation.LatencyCompensator;
import io.windfall.anticheat.core.compensation.PingPongManager;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import io.windfall.anticheat.core.fingerprint.PacketFingerprint;
import io.windfall.anticheat.core.metrics.WindfallPrometheus;
import io.windfall.anticheat.core.severity.ViolationPattern;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CheckManager {
    private final WindfallMod mod;
    private final List<Check> checks = new ArrayList<>();
    private final Map<String, Check> checkByKey = new ConcurrentHashMap<>();
    private final int serverProtocol;
    private long tickCounter = 0;
    private final PingPongManager pingPongManager;
    private final LatencyCompensator latencyCompensator;
    private final SimulationEngine simulationEngine;
    private final AdaptiveThreshold adaptiveThreshold;
    private final ViolationPattern violationPattern;
    private final PacketFingerprint packetFingerprint;
    private final WindfallPrometheus prometheus;

    public CheckManager(WindfallMod mod) {
        this.mod = mod;
        this.serverProtocol = mod.getVersionManager().getProtocolVersion();
        this.pingPongManager = mod.getPingPongManager();
        this.latencyCompensator = mod.getLatencyCompensator();
        this.simulationEngine = mod.getSimulationEngine();
        this.adaptiveThreshold = AdaptiveThreshold.getInstance();
        this.adaptiveThreshold.loadConfig(mod.getWindfallConfig());
        this.violationPattern = new ViolationPattern(mod.getConfigDir(), WindfallMod.LOGGER);
        this.violationPattern.loadConfig(true, 30, 3, 6000);
        this.packetFingerprint = new PacketFingerprint();
        this.packetFingerprint.loadConfig(true, 60, 6000);
        this.prometheus = new WindfallPrometheus(mod);
        registerChecks();
    }

    private void registerChecks() {
        List<Check> allChecks = new ArrayList<>();
        allChecks.add(new AimCheck());
        allChecks.add(new AutoclickerCheck());
        allChecks.add(new BacktrackCheck());
        allChecks.add(new HitboxesCheck());
        allChecks.add(new MultiInteractCheck());
        allChecks.add(new SelfInteractCheck());
        allChecks.add(new ReachCheck());
        allChecks.add(new CriticalsCheck());
        allChecks.add(new KillAuraCheck());
        allChecks.add(new FastHealCheck());
        allChecks.add(new SwordBlockCheck());
        allChecks.add(new MacroCheck());
        allChecks.add(new SpeedCheck());
        allChecks.add(new FlightCheck());
        allChecks.add(new VelocityCheck());
        allChecks.add(new TimerCheck());
        allChecks.add(new NoFallCheck());
        allChecks.add(new StepCheck());
        allChecks.add(new ScaffoldCheck());
        allChecks.add(new ElytraCheck());
        allChecks.add(new BaritoneCheck());
        allChecks.add(new GroundSpoofCheck());
        allChecks.add(new PhaseCheck());
        allChecks.add(new SimulationCheck());
        allChecks.add(new NoSlowCheck());
        allChecks.add(new MotionCheck());
        allChecks.add(new FastBreakCheck());
        allChecks.add(new FarBreakCheck());
        allChecks.add(new FarPlaceCheck());
        allChecks.add(new InvalidBreakCheck());
        allChecks.add(new InvalidPlaceCheck());
        allChecks.add(new NoSwingCheck());
        allChecks.add(new RotationBreakCheck());
        allChecks.add(new AirLiquidBreakCheck());
        allChecks.add(new WrongBreakCheck());
        allChecks.add(new PositionBreakCheck());
        allChecks.add(new MultiBreakCheck());
        allChecks.add(new AirLiquidPlaceCheck());
        allChecks.add(new RotationPlaceCheck());
        allChecks.add(new PositionPlaceCheck());
        allChecks.add(new MultiPlaceCheck());
        allChecks.add(new BadPacketsCheck());
        allChecks.add(new ChestStealerCheck());
        allChecks.add(new CreativeCheck());
        allChecks.add(new PacketOrderCheck());
        allChecks.add(new ChatCheck());
        allChecks.add(new CrashCheck());
        allChecks.add(new SprintCheck());
        allChecks.add(new ExploitCheck());
        allChecks.add(new ClientBrandCheck());
        allChecks.add(new VehicleCheck());
        allChecks.add(new InventoryCheck());
        allChecks.add(new TransactionCheck());

        for (Check check : allChecks) {
            checks.add(check);
            checkByKey.put(check.getStableKey(), check);
        }
        WindfallMod.LOGGER.info("[Windfall] Registered {}/{} checks for Fabric", checks.size(), allChecks.size());
    }

    public void onPacketReceive(WindfallPlayer player, Object event) {
        packetFingerprint.recordPacketInterval(player.getUuid(), 50);
        for (Check check : checks) {
            if (!check.isEnabled()) continue;
            try { check.onPacketReceive(player, event); } catch (Exception e) { WindfallMod.LOGGER.debug("Check {} error: {}", check.getName(), e.getMessage()); }
        }
    }

    public void onPacketSend(WindfallPlayer player, Object event) {
        for (Check check : checks) {
            if (!check.isEnabled()) continue;
            try { check.onPacketSend(player, event); } catch (Exception e) { WindfallMod.LOGGER.debug("Check {} error: {}", check.getName(), e.getMessage()); }
        }
    }

    public void onTick() {
        adaptiveThreshold.onTick(50);
        prometheus.tick();
        for (WindfallPlayer player : mod.getPlayerManager().getAllPlayers()) {
            if (!player.isValid()) continue;
            pingPongManager.onTickStart(player);
            player.resetTickState();
            player.getActionData().tick();
            player.updateCachedState();
            latencyCompensator.processDeferredChanges(player.getUuid(), player);
            for (Check check : checks) {
                if (!check.isEnabled()) continue;
                check.reward(player);
                if (check instanceof ScaffoldCheck) ((ScaffoldCheck) check).onTick(player, tickCounter);
                if (check instanceof TransactionCheck) ((TransactionCheck) check).onTick(player);
            }
            if (mod.getPunishmentEngine() != null) mod.getPunishmentEngine().decayTierIfNeeded(player);
            pingPongManager.onTickEnd(player);
        }
        if (tickCounter++ % 200 == 0) ReachCheck.cleanup(10_000L);
        if (tickCounter % 6000 == 0) {
            if (mod.getAlertManager() != null) mod.getAlertManager().getDiscordWebhook().cleanupStaleEntries();
            packetFingerprint.onTick(tickCounter);
            violationPattern.pruneOldHistories();
            latencyCompensator.pruneTickHistory((int) tickCounter);
        }
    }

    public void reloadChecks() {
        mod.getWindfallConfig().reload();
        for (Check check : checks) {
            check.setEnabled(mod.getWindfallConfig().isCheckEnabled(check.getStableKey()));
            check.setPunishable(mod.getWindfallConfig().isCheckPunishable(check.getStableKey()));
        }
    }

    public Check getCheckByStableKey(String key) { return checkByKey.get(key); }
    public List<Check> getChecks() { return checks; }
    public int getServerProtocol() { return serverProtocol; }
    public long getTickCounter() { return tickCounter; }
    public PingPongManager getPingPongManager() { return pingPongManager; }
    public LatencyCompensator getLatencyCompensator() { return latencyCompensator; }
    public SimulationEngine getSimulationEngine() { return simulationEngine; }
    public AdaptiveThreshold getAdaptiveThreshold() { return adaptiveThreshold; }
    public ViolationPattern getViolationPattern() { return violationPattern; }
    public PacketFingerprint getPacketFingerprint() { return packetFingerprint; }
    public WindfallPrometheus getPrometheus() { return prometheus; }

    public void removePlayer(UUID uuid) {
        for (Check check : checks) {
            try { check.removePlayer(uuid); } catch (Exception e) { WindfallMod.LOGGER.debug("Failed to remove player from {}: {}", check.getName(), e.getMessage()); }
        }
    }
}
