package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

@CheckData(name = "Air Liquid Break", stableKey = "windfall.movement.airliquidbreak", decay = 0.02, setbackVl = 10)
public class AirLiquidBreakCheck extends Check implements PacketCheck {

    private static final int BUFFER_THRESHOLD = 3;

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        if (action.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return;

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
