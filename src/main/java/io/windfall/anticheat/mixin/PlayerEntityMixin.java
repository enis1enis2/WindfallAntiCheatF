package io.windfall.anticheat.mixin;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "travel", at = @At("HEAD"))
    private void windfall_travel(Vec3d movement, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof net.minecraft.server.network.ServerPlayerEntity)) return;
        net.minecraft.server.network.ServerPlayerEntity sp = (net.minecraft.server.network.ServerPlayerEntity) self;
        WindfallMod mod = WindfallMod.getInstance();
        if (mod == null) return;
        PlayerManager pm = mod.getPlayerManager();
        WindfallPlayer wp = pm.get(sp.getUuid());
        if (wp != null) {
            wp.setSprinting(sp.isSprinting());
            wp.setGliding(sp.isFallFlying());
        }
    }
}
