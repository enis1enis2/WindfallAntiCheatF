package io.windfall.anticheat.core.plugin;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger("Windfall");

    private boolean viaVersionInstalled;
    private boolean viaBackwardsInstalled;
    private boolean geyserInstalled;
    private boolean oldCombatMechanicsInstalled;

    public void init() {
        viaVersionInstalled = isModLoaded("viaversion");
        if (viaVersionInstalled) {
            LOGGER.info("[Windfall] ViaVersion detected — per-player protocol adaptation active");
        }

        viaBackwardsInstalled = isModLoaded("viabackwards");
        if (viaBackwardsInstalled) {
            LOGGER.info("[Windfall] ViaBackwards detected — backward protocol translation active");
        }

        geyserInstalled = isModLoaded("geyser-fabric") || isModLoaded("geyser");
        if (geyserInstalled) {
            LOGGER.info("[Windfall] Geyser detected — Bedrock player adaptation active");
            String geyserVersion = getModVersion("geyser-fabric");
            if (geyserVersion == null) geyserVersion = getModVersion("geyser");
            if (geyserVersion != null && isGeyserVulnerable(geyserVersion)) {
                LOGGER.warn("[Windfall] Geyser {} has known SSRF vulnerability (CVE-2026-42188) — update to 2.9.3+", geyserVersion);
            }
        }

        oldCombatMechanicsInstalled = isModLoaded("oldcombatmechanics");
        if (oldCombatMechanicsInstalled) {
            LOGGER.info("[Windfall] OldCombatMechanics detected — 1.8 combat emulation active");
        }
    }

    private boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    private String getModVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse(null);
    }

    public boolean isViaVersionInstalled() { return viaVersionInstalled; }
    public boolean isViaBackwardsInstalled() { return viaBackwardsInstalled; }
    public boolean isGeyserInstalled() { return geyserInstalled; }
    public boolean isOldCombatMechanicsInstalled() { return oldCombatMechanicsInstalled; }
    public boolean isAnyViaVersionPlugin() { return viaVersionInstalled || viaBackwardsInstalled; }

    private boolean isGeyserVulnerable(String version) {
        try {
            String[] parts = version.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return major < 2 || (major == 2 && minor < 9) || (major == 2 && minor == 9 && patch < 3);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
