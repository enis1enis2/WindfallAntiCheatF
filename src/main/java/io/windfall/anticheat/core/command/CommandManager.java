package io.windfall.anticheat.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.Check;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandManager {
    private final WindfallMod mod;

    public CommandManager(WindfallMod mod) {
        this.mod = mod;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerCommands(dispatcher);
        });
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("windfall")
            .then(literal("reload")
                .executes(ctx -> {
                    mod.getCheckManager().reloadChecks();
                    ctx.getSource().sendFeedback(() -> Text.literal("§a[Windfall] Config reloaded!"), true);
                    return 1;
                })
            )
            .then(literal("alerts")
                .executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                    if (player != null) {
                        WindfallPlayer wp = mod.getPlayerManager().get(player.getUuid());
                        if (wp != null) {
                            wp.setAlertsEnabled(!wp.isAlertsEnabled());
                            ctx.getSource().sendFeedback(() -> Text.literal("§a[Windfall] Alerts " + (wp.isAlertsEnabled() ? "enabled" : "disabled")), false);
                        }
                    }
                    return 1;
                })
            )
            .then(literal("status")
                .executes(ctx -> {
                    int players = mod.getPlayerManager().size();
                    int checks = mod.getCheckManager().getChecks().size();
                    ctx.getSource().sendFeedback(() -> Text.literal("§b[Windfall] §7Players: " + players + " | Checks: " + checks + " | Tick: " + mod.getCheckManager().getTickCounter()), false);
                    return 1;
                })
            )
            .then(literal("check")
                .then(argument("player", net.minecraft.command.argument.EntityArgumentType.player())
                    .executes(ctx -> {
                        ServerPlayerEntity target = net.minecraft.command.argument.EntityArgumentType.getPlayer(ctx, "player");
                        WindfallPlayer wp = mod.getPlayerManager().get(target.getUuid());
                        if (wp != null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("§b=== ").append(target.getName().getString()).append(" ===\n");
                            sb.append("§7Ping: §f").append(wp.getTransactionPing()).append("ms\n");
                            sb.append("§7VL: §f").append(wp.getTotalViolationLevel()).append("\n");
                            for (var entry : wp.getViolationLevels().entrySet()) {
                                if (entry.getValue() > 0) sb.append("§c  ").append(entry.getKey()).append(": §f").append(entry.getValue()).append("\n");
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                        }
                        return 1;
                    })
                )
            )
        );
    }
}
