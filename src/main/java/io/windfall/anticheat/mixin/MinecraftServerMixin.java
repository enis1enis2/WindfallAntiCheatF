package io.windfall.anticheat.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    // TPS tracking handled via server tick events — no mixin needed
}
