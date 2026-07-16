package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="AirLiquidPlace A", stableKey="windfall.movement.airliquidplace", decay=0.02, setbackVl=20)
public class AirLiquidPlaceCheck extends Check implements PacketCheck {

    private static final long FLAG_COOLDOWN_MS = 1000;

    private final ConcurrentHashMap<UUID, Long> lastFlagTime = new ConcurrentHashMap<>();

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerInteractBlockC2SPacket)) return;

        PlayerInteractBlockC2SPacket p = (PlayerInteractBlockC2SPacket) packet;
        BlockPos blockPos = p.getBlockHitResult().getBlockPos();
        Direction face = p.getBlockHitResult().getSide();

        BlockPos placePos = blockPos.offset(face);

        ServerWorld world;
        try {
            world = (ServerWorld) player.getServerPlayer().getWorld();
        } catch (Exception e) {
            return;
        }

        boolean hasAdjacentSolid = false;
        boolean hasAdjacentLiquid = false;

        for (Direction dir : Direction.values()) {
            BlockPos neighbor = placePos.offset(dir);
            BlockState neighborState = world.getBlockState(neighbor);
            FluidState neighborFluid = neighborState.getFluidState();

            if (!neighborState.isAir() && !neighborFluid.isEmpty()) {
                hasAdjacentLiquid = true;
            }
            if (neighborState.isSolidBlock(world, neighbor)) {
                hasAdjacentSolid = true;
                break;
            }
        }

        long now = System.currentTimeMillis();
        Long last = lastFlagTime.get(player.getUuid());
        if (last != null && now - last < FLAG_COOLDOWN_MS) return;

        if (!hasAdjacentSolid) {
            increaseBuffer(player, 1.5);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
                lastFlagTime.put(player.getUuid(), now);
            }
        } else if (hasAdjacentLiquid) {
            BlockState targetState = world.getBlockState(placePos);
            boolean targetIsAir = targetState.isAir();
            boolean targetIsLiquid = !targetState.getFluidState().isEmpty();

            if (targetIsLiquid || targetIsAir) {
                FluidState placeFluid = targetState.getFluidState();
                boolean placingIntoLiquid = placeFluid.isOf(net.minecraft.fluid.Fluids.WATER)
                        || placeFluid.isOf(net.minecraft.fluid.Fluids.LAVA);
                if (placingIntoLiquid) {
                    increaseBuffer(player, 0.8);
                    if (getBuffer(player) > 3.0) {
                        flag(player);
                        resetBuffer(player);
                        lastFlagTime.put(player.getUuid(), now);
                    }
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
