package io.windfall.anticheat;

import io.windfall.anticheat.core.alert.AlertManager;
import io.windfall.anticheat.api.WindfallAPI;
import io.windfall.anticheat.api.WindfallProvider;
import io.windfall.anticheat.api.WindfallAPIImpl;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.command.CommandManager;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.network.PacketListener;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.punishment.PunishmentEngine;
import io.windfall.anticheat.core.severity.SeverityManager;
import io.windfall.anticheat.core.version.VersionManager;
import io.windfall.anticheat.core.compensation.TransactionManager;
import io.windfall.anticheat.core.compensation.PingPongManager;
import io.windfall.anticheat.core.compensation.LatencyCompensator;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class WindfallMod implements ModInitializer {
    public static final String MOD_ID = "windfall-fabric";
    public static final Logger LOGGER = LoggerFactory.getLogger("Windfall");

    private static WindfallMod instance;
    private MinecraftServer server;
    private WindfallConfig config;
    private VersionManager versionManager;
    private PlayerManager playerManager;
    private CheckManager checkManager;
    private TransactionManager transactionManager;
    private PingPongManager pingPongManager;
    private LatencyCompensator latencyCompensator;
    private SimulationEngine simulationEngine;
    private CommandManager commandManager;
    private AlertManager alertManager;
    private PunishmentEngine punishmentEngine;
    private SeverityManager severityManager;
    private volatile boolean running;

    @Override
    public void onInitialize() {
        instance = this;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            this.server = server;
            onEnable();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> onDisable());
    }

    private void onEnable() {
        long start = System.nanoTime();
        this.config = new WindfallConfig(this);
        this.versionManager = new VersionManager();
        this.playerManager = new PlayerManager();
        this.transactionManager = new TransactionManager(this);
        this.pingPongManager = new PingPongManager(this);
        this.latencyCompensator = new LatencyCompensator();
        this.simulationEngine = new SimulationEngine(pingPongManager, latencyCompensator);
        this.severityManager = SeverityManager.fromConfig(config);
        this.punishmentEngine = new PunishmentEngine(this);
        this.checkManager = new CheckManager(this);
        this.commandManager = new CommandManager(this);
        this.alertManager = new AlertManager(this);

        WindfallProvider.register(new WindfallAPIImpl(this));

        PacketListener.register(this);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {});
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            java.util.UUID uuid = handler.getPlayer().getUuid();
            if (transactionManager != null) transactionManager.onPlayerQuit(uuid);
            if (pingPongManager != null) pingPongManager.onPlayerQuit(uuid);
            if (latencyCompensator != null) latencyCompensator.onPlayerQuit(uuid);
            if (punishmentEngine != null) punishmentEngine.cleanup(uuid);
            if (checkManager != null) {
                checkManager.removePlayer(uuid);
                checkManager.getViolationPattern().savePlayerHistory(uuid);
                checkManager.getPacketFingerprint().removePlayer(uuid);
            }
            if (playerManager != null) playerManager.remove(uuid);
        });

        this.running = true;
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);

        long elapsed = (System.nanoTime() - start) / 1_000_000;
        LOGGER.info("Windfall F v1.0.0 enabled in {}ms", elapsed);
    }

    private void onTick(MinecraftServer server) {
        if (checkManager != null) checkManager.onTick();
    }

    private void onDisable() {
        this.running = false;
        WindfallProvider.unregister();
        LOGGER.info("Windfall F disabled.");
    }

    public static WindfallMod getInstance() { return instance; }
    public MinecraftServer getServer() { return server; }
    public WindfallConfig getWindfallConfig() { return config; }
    public VersionManager getVersionManager() { return versionManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public CheckManager getCheckManager() { return checkManager; }
    public TransactionManager getTransactionManager() { return transactionManager; }
    public PingPongManager getPingPongManager() { return pingPongManager; }
    public LatencyCompensator getLatencyCompensator() { return latencyCompensator; }
    public SimulationEngine getSimulationEngine() { return simulationEngine; }
    public CommandManager getCommandManager() { return commandManager; }
    public AlertManager getAlertManager() { return alertManager; }
    public PunishmentEngine getPunishmentEngine() { return punishmentEngine; }
    public SeverityManager getSeverityManager() { return severityManager; }
    public boolean isRunning() { return running; }
    public Path getConfigDir() { return FabricLoader.getInstance().getConfigDir(); }
}
