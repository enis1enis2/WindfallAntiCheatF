package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

@CheckData(name = "Air Liquid Place", stableKey = "windfall.movement.airliquidplace", decay = 0.02, setbackVl = 10)
public class AirLiquidPlaceCheck extends Check implements PacketCheck {

    private static final int BUFFER_THRESHOLD = 3;

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerInteractBlockC2SPacket)) return;

        PlayerInteractBlockC2SPacket p = (PlayerInteractBlockC2SPacket) packet;
        if (p.getBlockHitResult().getSide() == null) return;

        ServerWorld world;
        try {
            world = (ServerWorld) player.getServerPlayer().getWorld();
        } catch (Exception e) {
            return;
        }

        BlockPos feetPos = new BlockPos(
            (int) Math.floor(player.getX()),
            (int) Math.floor(player.getY()),
            (int) Math.floor(player.getZ()));

        BlockState feetState = world.getBlockState(feetPos);
        FluidState feetFluid = feetState.getFluidState();

        boolean inLiquid = feetFluid.isOf(net.minecraft.fluid.Fluids.WATER)
                || feetFluid.isOf(net.minecraft.fluid.Fluids.LAVA);

        boolean inAir = feetState.isAir() && !inLiquid;

        boolean falling = player.getDeltaY() < -0.5;

        if (inLiquid || (inAir && falling)) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.5);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
