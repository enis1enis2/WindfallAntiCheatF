package io.windfall.anticheat.core.network;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.CheckManager;
import io.windfall.anticheat.core.compensation.PingPongManager;
import io.windfall.anticheat.core.compensation.TransactionManager;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class PacketListener {
    public static void register(WindfallMod mod) {
        MinecraftServer server = mod.getServer();
        PlayerManager pm = mod.getPlayerManager();
        TransactionManager tm = mod.getTransactionManager();

        // Client → Server: Position
        ServerPlayNetworking.registerGlobalReceiver(UpdatePlayerPositionC2SPacket.ID, (packet, context) -> {
            ServerPlayerEntity sp = context.player();
            WindfallPlayer wp = pm.get(sp.getUuid());
            if (wp == null || !wp.isValid()) return;
            if (wp.isRespawned()) wp.setRespawned(false);
            wp.setPosition(sp.getX(), sp.getY(), sp.getZ());
            wp.setOnGround(sp.isOnGround());
            wp.setMovedSinceTick(true);
            mod.getCheckManager().onPacketReceive(wp, packet);
        });

        // Client → Server: Position and Rotation
        ServerPlayNetworking.registerGlobalReceiver(UpdatePlayerLookC2SPacket.ID, (packet, context) -> {
            ServerPlayerEntity sp = context.player();
            WindfallPlayer wp = pm.get(sp.getUuid());
            if (wp == null || !wp.isValid()) return;
            wp.setYaw(sp.getYaw());
            wp.setPitch(sp.getPitch());
            wp.setOnGround(sp.isOnGround());
            wp.setMovedSinceTick(true);
            mod.getCheckManager().onPacketReceive(wp, packet);
        });

        // Client → Server: Rotation only
        ServerPlayNetworking.registerGlobalReceiver(LookDirectionC2SPacket.ID, (packet, context) -> {
            ServerPlayerEntity sp = context.player();
            WindfallPlayer wp = pm.get(sp.getUuid());
            if (wp == null || !wp.isValid()) return;
            wp.setYaw(sp.getYaw());
            wp.setPitch(sp.getPitch());
            wp.setOnGround(sp.isOnGround());
            mod.getCheckManager().onPacketReceive(wp, packet);
        });

        // Client → Server: Keep Alive
        ServerPlayNetworking.registerGlobalReceiver(KeepAliveC2SPacket.ID, (packet, context) -> {
            ServerPlayerEntity sp = context.player();
            WindfallPlayer wp = pm.get(sp.getUuid());
            if (wp == null || !wp.isValid()) return;
            tm.processTransaction(wp, (short) (packet.readLong() & 0xFFFF));
            mod.getCheckManager().onPacketReceive(wp, packet);
        });

        // Client → Server: All other packets
        ServerPlayNetworking.registerGlobalReceiver(CustomPayloadC2SPacket.ID, (packet, context) -> {
            ServerPlayerEntity sp = context.player();
            WindfallPlayer wp = pm.get(sp.getUuid());
            if (wp == null || !wp.isValid()) return;
            mod.getCheckManager().onPacketReceive(wp, packet);
        });

        // Connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server2) -> {
            ServerPlayerEntity sp = handler.getPlayer();
            if (pm.get(sp.getUuid()) != null) return;
            WindfallPlayer wp = new WindfallPlayer(sp);
            pm.add(wp);
        });

        // Server → Client: Entity Velocity
        // Intercept via mixin or event — handled in ServerPlayNetworkHandlerMixin
    }
}
