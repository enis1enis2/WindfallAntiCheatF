package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="AirLiquidBreak A", stableKey="windfall.movement.airliquidbreak", decay=0.02, setbackVl=20)
public class AirLiquidBreakCheck extends Check implements PacketCheck {

    private static final long FLAG_COOLDOWN_MS = 1000;

    private final ConcurrentHashMap<UUID, Long> lastFlagTime = new ConcurrentHashMap<>();

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        if (action.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return;

        BlockPos blockPos = action.getPos();

        ServerWorld world;
        try {
            world = (ServerWorld) player.getServerPlayer().getWorld();
        } catch (Exception e) {
            return;
        }

        BlockState blockState = world.getBlockState(blockPos);
        FluidState fluidState = blockState.getFluidState();

        boolean isAir = blockState.isAir();
        boolean isWater = fluidState.isOf(net.minecraft.fluid.Fluids.WATER);
        boolean isLava = fluidState.isOf(net.minecraft.fluid.Fluids.LAVA);

        if (isAir) {
            long now = System.currentTimeMillis();
            Long last = lastFlagTime.get(player.getUuid());
            if (last == null || now - last > FLAG_COOLDOWN_MS) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                    lastFlagTime.put(player.getUuid(), now);
                }
            }
        } else if (isWater || isLava) {
            long now = System.currentTimeMillis();
            Long last = lastFlagTime.get(player.getUuid());
            if (last == null || now - last > FLAG_COOLDOWN_MS) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 2.5) {
                    flag(player);
                    resetBuffer(player);
                    lastFlagTime.put(player.getUuid(), now);
                }
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        lastFlagTime.remove(uuid);
    }
}
