package io.windfall.anticheat.core.check;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.alert.AlertManager;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.metrics.WindfallPrometheus;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public abstract class Check {
    protected final String name;
    protected final String stableKey;
    protected volatile boolean enabled;
    protected volatile boolean punishable;
    protected volatile double decay;
    protected volatile int maxVl;
    protected volatile int setbackVl;
    protected volatile int minVersion;
    protected volatile int maxVersion;
    protected final CompatFlag[] compatFlags;
    protected final double relaxMultiplier;

    public Check() {
        CheckData data = getClass().getAnnotation(CheckData.class);
        if (data == null) {
            throw new IllegalStateException("Check " + getClass().getSimpleName() + " is missing @CheckData");
        }
        this.name = data.name();
        this.stableKey = data.stableKey();
        this.decay = data.decay();
        this.setbackVl = data.setbackVl();
        this.minVersion = data.minVersion();
        this.maxVersion = data.maxVersion();
        this.compatFlags = data.compat();
        this.relaxMultiplier = data.relaxMultiplier();

        WindfallConfig cfg = WindfallMod.getInstance().getWindfallConfig();
        this.enabled = cfg.isCheckEnabled(stableKey);
        this.maxVl = cfg.getCheckMaxVl(stableKey);
        this.punishable = cfg.isCheckPunishable(stableKey);
    }

    public abstract void onPacketReceive(WindfallPlayer player, Object packet);
    public abstract void onPacketSend(WindfallPlayer player, Object packet);

    public void removePlayer(java.util.UUID uuid) {}

    public void flag(WindfallPlayer player) {
        if (!enabled) return;
        WindfallMod plugin = WindfallMod.getInstance();
        int increment = plugin.getSeverityManager().getScaledVlIncrement(player);
        int vl = player.getViolationLevels().merge(stableKey, increment, Integer::sum);
        if (vl > maxVl) { player.getViolationLevels().put(stableKey, maxVl); vl = maxVl; }

        if (plugin.getCheckManager() != null) {
            plugin.getCheckManager().getViolationPattern()
                .recordViolation(player.getUuid(), stableKey, vl, plugin.getCheckManager().getTickCounter());
            WindfallPrometheus prometheus = plugin.getCheckManager().getPrometheus();
            if (prometheus != null) prometheus.incrementFlags(stableKey);
        }

        AlertManager alertManager = plugin.getAlertManager();
        if (alertManager != null && player.isAlertsEnabled() && vl > 0) {
            alertManager.sendAlert(player, this, "VL=" + vl);
        } else if (plugin.getWindfallConfig().isVerboseEnabled()) {
            WindfallMod.LOGGER.warn("[{}] {} VL={}", name, player.getName(), vl);
        }

        if (punishable && plugin.getPunishmentEngine() != null) {
            plugin.getPunishmentEngine().evaluate(player);
        }

        if (vl >= setbackVl) {
            player.getViolationLevels().put(stableKey, 0);
            performSetback(player);
        }
    }

    public void flagWithSetback(WindfallPlayer player) {
        if (!enabled) return;
        WindfallMod plugin = WindfallMod.getInstance();
        int increment = plugin.getSeverityManager().getScaledVlIncrement(player);
        int vl = player.getViolationLevels().merge(stableKey, increment, Integer::sum);
        if (vl > maxVl) { player.getViolationLevels().put(stableKey, maxVl); vl = maxVl; }

        AlertManager alertManager = plugin.getAlertManager();
        if (alertManager != null && player.isAlertsEnabled()) {
            alertManager.sendAlert(player, this, "VL=" + vl + " (SETBACK)");
        }
        if (punishable && plugin.getPunishmentEngine() != null) {
            plugin.getPunishmentEngine().evaluate(player);
        }
        performSetback(player);
        if (vl >= setbackVl) player.getViolationLevels().put(stableKey, 0);
    }

    public void reward(WindfallPlayer player) {
        int vl = player.getViolationLevels().getOrDefault(stableKey, 0);
        if (vl > 1) player.getViolationLevels().put(stableKey, vl - 1);
        else if (vl == 1) player.getViolationLevels().put(stableKey, 0);
        double buf = player.getBuffers().getOrDefault(stableKey, 0.0);
        if (buf > 0.0) player.getBuffers().put(stableKey, Math.max(0.0, buf - decay));
    }

    protected void performSetback(WindfallPlayer player) {
        if (player.isRespawned()) return;
        ServerPlayerEntity sp = player.getServerPlayer();
        if (sp == null || !sp.isAlive()) return;
        double tx = player.getTeleportX();
        double ty = player.getTeleportY();
        double tz = player.getTeleportZ();
        if (tx == 0.0 && ty == 0.0 && tz == 0.0) {
            tx = player.getGroundX();
            ty = player.getGroundY();
            tz = player.getGroundZ();
        }
        ServerWorld world = (ServerWorld) sp.getWorld();
        sp.teleportTo(new net.minecraft.world.TeleportTarget(world, new Vec3d(tx, ty, tz), Vec3d.ZERO, player.getYaw(), player.getPitch(), net.minecraft.world.TeleportTarget.NO_OP));
    }

    public int getViolationLevel(WindfallPlayer player) {
        return player.getViolationLevels().getOrDefault(stableKey, 0);
    }
    public double getBuffer(WindfallPlayer player) {
        return player.getBuffers().getOrDefault(stableKey, 0.0);
    }
    public void increaseBuffer(WindfallPlayer player, double amount) {
        player.getBuffers().merge(stableKey, amount, Double::sum);
    }
    public void decreaseBuffer(WindfallPlayer player, double amount) {
        player.getBuffers().merge(stableKey, 0.0, (a, b) -> Math.max(0.0, a - amount));
    }
    public void resetBuffer(WindfallPlayer player) {
        player.getBuffers().put(stableKey, 0.0);
    }
    public void flagIfAboveThreshold(WindfallPlayer player, double value, double threshold) {
        if (value > threshold) {
            increaseBuffer(player, (value - threshold) / threshold);
            if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    public String getName() { return name; }
    public String getStableKey() { return stableKey; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isPunishable() { return punishable; }
    public void setPunishable(boolean punishable) { this.punishable = punishable; }
    public double getDecay() { return decay; }
    public int getMaxVl() { return maxVl; }
    public int getSetbackVl() { return setbackVl; }
    public int getMinVersion() { return minVersion; }
    public int getMaxVersion() { return maxVersion; }
    public CompatFlag[] getCompatFlags() { return compatFlags; }
    public double getRelaxMultiplier() { return relaxMultiplier; }
    public boolean hasCompatFlag(CompatFlag flag) {
        for (CompatFlag f : compatFlags) { if (f == flag) return true; }
        return false;
    }
}
