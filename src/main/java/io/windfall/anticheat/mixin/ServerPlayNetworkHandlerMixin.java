package io.windfall.anticheat.mixin;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.compensation.TransactionManager;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.RespawnS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Inject(method = "onPlayerPosition", at = @At("HEAD"))
    private void windfall_onPlayerPosition(UpdatePlayerPositionC2SPacket packet, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.getPlayer();
        if (player == null) return;
        WindfallMod mod = WindfallMod.getInstance();
        if (mod == null) return;
        PlayerManager pm = mod.getPlayerManager();
        WindfallPlayer wp = pm.get(player.getUuid());
        if (wp == null || !wp.isValid()) return;
        if (wp.isRespawned()) wp.setRespawned(false);
        wp.setPosition(player.getX(), player.getY(), player.getZ());
        wp.setOnGround(player.isOnGround());
        wp.setMovedSinceTick(true);
        mod.getCheckManager().onPacketReceive(wp, packet);
        wp.getActionData().recordBlockPlace(0, 0, 0); // placeholder
    }

    @Inject(method = "onPlayerRotation", at = @At("HEAD"))
    private void windfall_onPlayerRotation(LookDirectionC2SPacket packet, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.getPlayer();
        if (player == null) return;
        WindfallMod mod = WindfallMod.getInstance();
        if (mod == null) return;
        PlayerManager pm = mod.getPlayerManager();
        WindfallPlayer wp = pm.get(player.getUuid());
        if (wp == null || !wp.isValid()) return;
        wp.setYaw(player.getYaw());
        wp.setPitch(player.getPitch());
        wp.setOnGround(player.isOnGround());
        mod.getCheckManager().onPacketReceive(wp, packet);
    }

    @Inject(method = "onPlayerMove", at = @At("HEAD"))
    private void windfall_onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.getPlayer();
        if (player == null) return;
        WindfallMod mod = WindfallMod.getInstance();
        if (mod == null) return;
        PlayerManager pm = mod.getPlayerManager();
        WindfallPlayer wp = pm.get(player.getUuid());
        if (wp == null || !wp.isValid()) return;
        if (wp.isRespawned()) wp.setRespawned(false);
        wp.setOnGround(player.isOnGround());
        mod.getCheckManager().onPacketReceive(wp, packet);
    }

    @Inject(method = "onClientCommand", at = @At("HEAD"))
    private void windfall_onClientCommand(ClientCommandC2SPacket packet, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.getPlayer();
        if (player == null) return;
        WindfallMod mod = WindfallMod.getInstance();
        if (mod == null) return;
        PlayerManager pm = mod.getPlayerManager();
        WindfallPlayer wp = pm.get(player.getUuid());
        if (wp == null || !wp.isValid()) return;
        mod.getCheckManager().onPacketReceive(wp, packet);
    }

    @Inject(method = "onInteractEntity", at = @At("HEAD"))
    private void windfall_onInteractEntity(InteractEntityC2SPacket packet, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.getPlayer();
        if (player == null) return;
        WindfallMod mod = WindfallMod.getInstance();
        if (mod == null) return;
        PlayerManager pm = mod.getPlayerManager();
        WindfallPlayer wp = pm.get(player.getUuid());
        if (wp == null || !wp.isValid()) return;
        wp.setLastAttackTime(System.currentTimeMillis());
        mod.getCheckManager().onPacketReceive(wp, packet);
    }

    @Inject(method = "onKeepAlive", at = @At("HEAD"))
    private void windfall_onKeepAlive(KeepAliveC2SPacket packet, CallbackInfo ci) {
        ServerPlayNetworkHandler handler = (ServerPlayNetworkHandler) (Object) this;
        ServerPlayerEntity player = handler.getPlayer();
        if (player == null) return;
        WindfallMod mod = WindfallMod.getInstance();
        if (mod == null) return;
        PlayerManager pm = mod.getPlayerManager();
        WindfallPlayer wp = pm.get(player.getUuid());
        if (wp == null || !wp.isValid()) return;
        mod.getTransactionManager().processTransaction(wp, (short) (packet.readLong() & 0xFFFF));
        mod.getCheckManager().onPacketReceive(wp, packet);
    }
}
