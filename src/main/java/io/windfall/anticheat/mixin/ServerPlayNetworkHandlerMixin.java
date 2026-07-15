package io.windfall.anticheat.mixin;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {

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
        wp.setPosition(player.getX(), player.getY(), player.getZ());
        wp.setYaw(player.getYaw());
        wp.setPitch(player.getPitch());
        wp.setOnGround(player.isOnGround());
        wp.setMovedSinceTick(true);
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

    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"))
    private void windfall_onInteractEntity(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
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

    @Inject(method = "onHandSwing", at = @At("HEAD"))
    private void windfall_onHandSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
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

    @Inject(method = "onPlayerAction", at = @At("HEAD"))
    private void windfall_onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
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

    @Inject(method = "onCloseHandledScreen", at = @At("HEAD"))
    private void windfall_onCloseScreen(CloseHandledScreenC2SPacket packet, CallbackInfo ci) {
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

    @Inject(method = "onClickSlot", at = @At("HEAD"))
    private void windfall_onClickSlot(ClickSlotC2SPacket packet, CallbackInfo ci) {
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

    @Inject(method = "onCreativeInventoryAction", at = @At("HEAD"))
    private void windfall_onCreative(CreativeInventoryActionC2SPacket packet, CallbackInfo ci) {
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

    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"))
    private void windfall_onInteractBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
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

    @Inject(method = "onPlayerInteractItem", at = @At("HEAD"))
    private void windfall_onInteractItem(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
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
}
