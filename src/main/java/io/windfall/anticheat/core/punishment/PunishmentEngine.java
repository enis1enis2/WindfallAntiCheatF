package io.windfall.anticheat.core.punishment;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentEngine {
    private final WindfallMod mod;
    private final Map<UUID, Integer> punishmentTier = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastWarnTime = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastKickTime = new ConcurrentHashMap<>();

    public PunishmentEngine(WindfallMod mod) { this.mod = mod; }

    public void evaluate(WindfallPlayer player) {
        WindfallConfig config = mod.getWindfallConfig();
        if (!config.isPunishmentsEnabled()) return;

        int totalVl = player.getTotalViolationLevel();
        int currentTier = punishmentTier.getOrDefault(player.getUuid(), 0);
        UUID uuid = player.getUuid();
        long now = System.currentTimeMillis();

        if (totalVl >= config.getPunishmentPermbanVl() && currentTier < 4) {
            ServerPlayerEntity sp = player.getServerPlayer();
            if (sp != null) {
                sp.networkHandler.disconnect(net.text.Text.literal(config.getPunishmentPermbanReason()));
            }
            punishmentTier.put(uuid, 4);
        } else if (totalVl >= config.getPunishmentTempbanVl() && currentTier < 3) {
            ServerPlayerEntity sp = player.getServerPlayer();
            if (sp != null) {
                sp.networkHandler.disconnect(net.text.Text.literal(config.getPunishmentTempbanReason()));
            }
            punishmentTier.put(uuid, 3);
        } else if (totalVl >= config.getPunishmentKickVl() && currentTier < 2) {
            ServerPlayerEntity sp = player.getServerPlayer();
            if (sp != null) {
                sp.networkHandler.disconnect(net.text.Text.literal(config.getPunishmentKickMessage()));
            }
            punishmentTier.put(uuid, 2);
            lastKickTime.put(uuid, now);
        } else if (totalVl >= config.getPunishmentWarnVl() && currentTier < 1) {
            ServerPlayerEntity sp = player.getServerPlayer();
            if (sp != null) {
                sp.sendMessage(net.text.Text.literal(config.getPunishmentWarnMessage()), false);
            }
            punishmentTier.put(uuid, 1);
            lastWarnTime.put(uuid, now);
        }
    }

    public void decayTierIfNeeded(WindfallPlayer player) {
        int totalVl = player.getTotalViolationLevel();
        WindfallConfig config = mod.getWindfallConfig();
        UUID uuid = player.getUuid();
        int tier = punishmentTier.getOrDefault(uuid, 0);
        if (tier == 4 && totalVl < config.getPunishmentPermbanVl()) punishmentTier.put(uuid, 3);
        if (tier == 3 && totalVl < config.getPunishmentTempbanVl()) punishmentTier.put(uuid, 2);
        if (tier == 2 && totalVl < config.getPunishmentKickVl()) punishmentTier.put(uuid, 1);
        if (tier == 1 && totalVl < config.getPunishmentWarnVl()) punishmentTier.put(uuid, 0);
    }

    public void cleanup(UUID uuid) {
        punishmentTier.remove(uuid);
        lastWarnTime.remove(uuid);
        lastKickTime.remove(uuid);
    }
}
