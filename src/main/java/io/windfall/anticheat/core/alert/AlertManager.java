package io.windfall.anticheat.core.alert;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class AlertManager {
    private final WindfallMod mod;
    private final DiscordWebhook discordWebhook;
    private long lastAlertTime = 0;

    public AlertManager(WindfallMod mod) {
        this.mod = mod;
        this.discordWebhook = new DiscordWebhook(mod);
    }

    public void sendAlert(WindfallPlayer player, Check check, String extra) {
        if (!mod.getWindfallConfig().isAlertsEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastAlertTime < 100) return;
        lastAlertTime = now;

        String message = mod.getWindfallConfig().getAlertPrefix()
            + player.getName() + " flagged " + check.getName() + " " + extra;

        if (mod.getWindfallConfig().isBroadcastToAllStaff()) {
            for (ServerPlayerEntity p : mod.getServer().getPlayerManager().getPlayerList()) {
                if (p.hasPermissionLevel(2) || p.getCommandSource().hasPermissionLevel(2)) {
                    p.sendMessage(Text.literal(message), false);
                }
            }
        }

        if (mod.getWindfallConfig().isDiscordEnabled()) {
            int vl = player.getViolationLevels().getOrDefault(check.getStableKey(), 0);
            discordWebhook.send(player.getName(), check.getName(), vl, extra);
        }
    }

    public DiscordWebhook getDiscordWebhook() { return discordWebhook; }
}
