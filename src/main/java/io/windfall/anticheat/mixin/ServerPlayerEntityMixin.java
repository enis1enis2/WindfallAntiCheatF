package io.windfall.anticheat.mixin;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.player.PlayerManager;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {

    @Inject(method = "setPose", at = @At("HEAD"))
    private void windfall_setPose(EntityPose pose, CallbackInfo ci) {
        ServerPlayerEntity self = (ServerPlayerEntity) (Object) this;
        WindfallMod mod = WindfallMod.getInstance();
        if (mod == null) return;
        PlayerManager pm = mod.getPlayerManager();
        WindfallPlayer wp = pm.get(self.getUuid());
        if (wp != null) {
            wp.setPose(WindfallPlayer.Pose.fromFabric(pose));
        }
    }
}
