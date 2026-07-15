package io.windfall.anticheat.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.windfall.anticheat.WindfallMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WindfallConfig {
    private final WindfallMod mod;
    private JsonObject config;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public WindfallConfig(WindfallMod mod) {
        this.mod = mod;
        if (mod == null) return;
        loadOrCreate();
    }

    private void loadOrCreate() {
        Path configPath = mod.getConfigDir().resolve("windfall.json");
        if (Files.exists(configPath)) {
            try {
                this.config = gson.fromJson(Files.newBufferedReader(configPath), JsonObject.class);
            } catch (Exception e) {
                WindfallMod.LOGGER.error("Failed to load config", e);
                this.config = new JsonObject();
            }
        } else {
            this.config = new JsonObject();
            setDefaults();
            save(configPath);
        }
        setDefaults();
        save(configPath);
    }

    private void setDefaults() {
        setDefault("alerts.enabled", true);
        setDefault("alerts.prefix", "\u00a78[\u00a7bWindfall\u00a78] \u00a77");
        setDefault("alerts.staff_permission", "windfall.alerts");
        setDefault("alerts.broadcast_to_all_staff", true);
        setDefault("discord.enabled", false);
        setDefault("discord.webhook_url", "");
        setDefault("discord.server_name", "My Server");
        setDefault("discord.mention_on_high_vl", true);
        setDefault("discord.mention_threshold", 25);
        setDefault("discord.avatar_url", "");
        setDefault("discord.embed_color_low", 16776960);
        setDefault("discord.embed_color_med", 16744448);
        setDefault("discord.embed_color_high", 16711680);
        setDefault("discord.rate_limit_ms", 5000);
        setDefault("verbose", true);
        setDefault("severity.enabled", true);
        setDefault("severity.moderate_vl", 10);
        setDefault("severity.high_vl", 25);
        setDefault("severity.extreme_vl", 50);
        setDefault("severity.moderate_multiplier", 1.3);
        setDefault("severity.high_multiplier", 1.6);
        setDefault("severity.extreme_multiplier", 2.0);
        setDefault("punishments.enabled", true);
        setDefault("punishments.warn_vl", 5);
        setDefault("punishments.kick_vl", 10);
        setDefault("punishments.tempban_vl", 20);
        setDefault("punishments.tempban_duration", "1d");
        setDefault("punishments.permban_vl", 30);
        setDefault("punishments.warn_message", "\u00a7c[Windfall] \u00a7eWarning: further cheating will result in a kick.");
        setDefault("punishments.kick_message", "\u00a7c[Windfall] Kicked for cheating.");
        setDefault("punishments.tempban_reason", "[Windfall] Temporarily banned for cheating.");
        setDefault("punishments.permban_reason", "[Windfall] Permanently banned for cheating.");
        setDefault("adaptive.enabled", true);
        setDefault("adaptive.tps_threshold", 19.0);
        setDefault("adaptive.scale_factor", 0.02);
        setDefault("adaptive.max_tolerance_multiplier", 2.0);
        setDefault("adaptive.safe_mode_threshold", 12.0);
        setDefault("prometheus.enabled", false);
        setDefault("prometheus.host", "127.0.0.1");
        setDefault("prometheus.port", 9211);
        setDefault("checks.default.enabled", true);
        setDefault("checks.default.max_vl", 100);
        setDefault("checks.default.setback_vl", 20);
        setDefault("checks.default.decay", 0.02);
        setDefault("checks.default.punishable", true);
    }

    private void setDefault(String path, Object value) {
        String[] parts = path.split("\\.");
        JsonObject current = config;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) {
                JsonObject child = new JsonObject();
                current.add(parts[i], child);
                current = child;
            } else {
                current = current.getAsJsonObject(parts[i]);
            }
        }
        if (!current.has(parts[parts.length - 1])) {
            if (value instanceof Boolean) current.addProperty(parts[parts.length - 1], (Boolean) value);
            else if (value instanceof Integer) current.addProperty(parts[parts.length - 1], (Integer) value);
            else if (value instanceof Double) current.addProperty(parts[parts.length - 1], (Double) value);
            else if (value instanceof String) current.addProperty(parts[parts.length - 1], (String) value);
        }
    }

    private Object get(String path, Object def) {
        String[] parts = path.split("\\.");
        JsonObject current = config;
        for (int i = 0; i < parts.length - 1; i++) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) return def;
            current = current.getAsJsonObject(parts[i]);
        }
        if (!current.has(parts[parts.length - 1])) return def;
        com.google.gson.JsonElement e = current.get(parts[parts.length - 1]);
        if (def instanceof Boolean && e.isJsonPrimitive()) return e.getAsBoolean();
        if (def instanceof Integer && e.isJsonPrimitive()) return e.getAsInt();
        if (def instanceof Double && e.isJsonPrimitive()) return e.getAsDouble();
        if (def instanceof String && e.isJsonPrimitive()) return e.getAsString();
        return def;
    }

    private void save(Path path) {
        try { Files.writeString(path, gson.toJson(config)); }
        catch (IOException e) { WindfallMod.LOGGER.error("Failed to save config", e); }
    }

    public void reload() { loadOrCreate(); }

    public boolean isCheckEnabled(String key) { return (Boolean) get("checks." + key + ".enabled", (Boolean) get("checks.default.enabled", true)); }
    public int getCheckMaxVl(String key) { return (Integer) get("checks." + key + ".max_vl", (Integer) get("checks.default.max_vl", 100)); }
    public double getCheckDecay(String key) { return (Double) get("checks." + key + ".decay", (Double) get("checks.default.decay", 0.02)); }
    public boolean isCheckPunishable(String key) { return (Boolean) get("checks." + key + ".punishable", (Boolean) get("checks.default.punishable", true)); }

    public boolean isAlertsEnabled() { return (Boolean) get("alerts.enabled", true); }
    public String getAlertPrefix() { return (String) get("alerts.prefix", "\u00a78[\u00a7bWindfall\u00a78] \u00a77"); }
    public String getAlertsStaffPermission() { return (String) get("alerts.staff_permission", "windfall.alerts"); }
    public boolean isBroadcastToAllStaff() { return (Boolean) get("alerts.broadcast_to_all_staff", true); }
    public boolean isDiscordEnabled() { return (Boolean) get("discord.enabled", false); }
    public String getDiscordWebhookUrl() { return (String) get("discord.webhook_url", ""); }
    public String getDiscordServerName() { return (String) get("discord.server_name", "My Server"); }
    public boolean isDiscordMentionOnHighVl() { return (Boolean) get("discord.mention_on_high_vl", true); }
    public int getDiscordMentionThreshold() { return (Integer) get("discord.mention_threshold", 25); }
    public String getDiscordAvatarUrl() { return (String) get("discord.avatar_url", ""); }
    public int getDiscordEmbedColor(int vl) {
        if (vl >= getDiscordMentionThreshold()) return (Integer) get("discord.embed_color_high", 16711680);
        else if (vl >= 10) return (Integer) get("discord.embed_color_med", 16744448);
        return (Integer) get("discord.embed_color_low", 16776960);
    }
    public long getDiscordRateLimitMs() { return ((Integer) get("discord.rate_limit_ms", 5000)).longValue(); }
    public boolean isVerboseEnabled() { return (Boolean) get("verbose", true); }
    public boolean isSeverityEnabled() { return (Boolean) get("severity.enabled", true); }
    public int getSeverityModerateVl() { return (Integer) get("severity.moderate_vl", 10); }
    public int getSeverityHighVl() { return (Integer) get("severity.high_vl", 25); }
    public int getSeverityExtremeVl() { return (Integer) get("severity.extreme_vl", 50); }
    public double getSeverityModerateMultiplier() { return (Double) get("severity.moderate_multiplier", 1.3); }
    public double getSeverityHighMultiplier() { return (Double) get("severity.high_multiplier", 1.6); }
    public double getSeverityExtremeMultiplier() { return (Double) get("severity.extreme_multiplier", 2.0); }
    public boolean isPunishmentsEnabled() { return (Boolean) get("punishments.enabled", true); }
    public int getPunishmentWarnVl() { return (Integer) get("punishments.warn_vl", 5); }
    public int getPunishmentKickVl() { return (Integer) get("punishments.kick_vl", 10); }
    public int getPunishmentTempbanVl() { return (Integer) get("punishments.tempban_vl", 20); }
    public String getPunishmentTempbanDuration() { return (String) get("punishments.tempban_duration", "1d"); }
    public int getPunishmentPermbanVl() { return (Integer) get("punishments.permban_vl", 30); }
    public String getPunishmentWarnMessage() { return (String) get("punishments.warn_message", "\u00a7c[Windfall] \u00a7eWarning."); }
    public String getPunishmentKickMessage() { return (String) get("punishments.kick_message", "\u00a7c[Windfall] Kicked for cheating."); }
    public String getPunishmentTempbanReason() { return (String) get("punishments.tempban_reason", "[Windfall] Temp banned."); }
    public String getPunishmentPermbanReason() { return (String) get("punishments.permban_reason", "[Windfall] Perma banned."); }
    public boolean isAdaptiveEnabled() { return (Boolean) get("adaptive.enabled", true); }
    public double getAdaptiveTpsThreshold() { return (Double) get("adaptive.tps_threshold", 19.0); }
    public double getAdaptiveScaleFactor() { return (Double) get("adaptive.scale_factor", 0.02); }
    public double getAdaptiveMaxToleranceMultiplier() { return (Double) get("adaptive.max_tolerance_multiplier", 2.0); }
    public double getAdaptiveSafeModeThreshold() { return (Double) get("adaptive.safe_mode_threshold", 12.0); }
    public boolean isPrometheusEnabled() { return (Boolean) get("prometheus.enabled", false); }
    public String getPrometheusHost() { return (String) get("prometheus.host", "127.0.0.1"); }
    public int getPrometheusPort() { return (Integer) get("prometheus.port", 9211); }
}
