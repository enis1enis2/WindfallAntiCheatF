package io.windfall.anticheat.core.network;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.compensation.TransactionManager;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class PacketListener {
    public static void register(WindfallMod mod) {
        MinecraftServer server = mod.getServer();
        PlayerManager pm = mod.getPlayerManager();
        TransactionManager tm = mod.getTransactionManager();

        // Connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server2) -> {
            ServerPlayerEntity sp = handler.getPlayer();
            if (pm.get(sp.getUuid()) != null) return;
            WindfallPlayer wp = new WindfallPlayer(sp);
            pm.add(wp);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server2) -> {
            // Cleanup is handled in WindfallMod
        });
    }
}
