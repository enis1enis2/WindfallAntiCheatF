package io.windfall.anticheat.mixin;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"))
    private void windfall_onSendPacket(Packet<?> packet, CallbackInfo ci) {
        ClientConnection connection = (ClientConnection) (Object) this;
        PacketListener listener = connection.getPacketListener();
        if (!(listener instanceof ServerPlayNetworkHandler)) return;
        ServerPlayerEntity player = ((ServerPlayNetworkHandler) listener).getPlayer();
        if (player == null) return;
        WindfallMod mod = WindfallMod.getInstance();
        if (mod == null) return;
        PlayerManager pm = mod.getPlayerManager();
        WindfallPlayer wp = pm.get(player.getUuid());
        if (wp == null || !wp.isValid()) return;
        mod.getCheckManager().onPacketSend(wp, packet);
    }
}
