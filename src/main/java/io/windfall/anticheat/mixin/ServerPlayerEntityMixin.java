package io.windfall.anticheat.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    // Pose tracking moved to WindfallPlayer — no mixin needed
}
