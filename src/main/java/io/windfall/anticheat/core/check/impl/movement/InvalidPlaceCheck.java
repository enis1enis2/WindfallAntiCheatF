package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="InvalidPlace A", stableKey="windfall.movement.invalidplace", decay=0.02, setbackVl=20)
public class InvalidPlaceCheck extends Check implements PacketCheck {

    private static final int MAX_PLACEMENTS_PER_TICK = 4;
    private static final long TICK_WINDOW_MS = 50;

    private final Map<UUID, PlacementState> playerStates = new ConcurrentHashMap<>();

    private static final class PlacementState {
        int placementsThisTick;
        long tickStartTime;
        long lastFlagTime;
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof PlayerInteractBlockC2SPacket)) return;

        PlacementState state = playerStates.computeIfAbsent(player.getUuid(), k -> new PlacementState());
        long now = System.currentTimeMillis();

        if (now - state.tickStartTime > TICK_WINDOW_MS) {
            state.placementsThisTick = 0;
            state.tickStartTime = now;
        }

        state.placementsThisTick++;
        if (state.placementsThisTick > MAX_PLACEMENTS_PER_TICK) {
            flagIfNeeded(player, state, now, "rate_limit");
            return;
        }

        PlayerInteractBlockC2SPacket interact = (PlayerInteractBlockC2SPacket) packet;
        BlockHitResult hitResult = interact.getBlockHitResult();
        BlockPos blockPos = hitResult.getBlockPos();

        if (player.getServerPlayer() == null || player.getServerPlayer().getWorld() == null) return;
        BlockState blockState = player.getServerPlayer().getWorld().getBlockState(blockPos);

        boolean isAir = blockState.isOf(Blocks.AIR)
                || blockState.isOf(Blocks.CAVE_AIR)
                || blockState.isOf(Blocks.VOID_AIR);

        if (!isAir) {
            flagIfNeeded(player, state, now, "occupied");
            return;
        }

        Box playerBox = player.getServerPlayer().getBoundingBox();
        Box blockBox = new Box(blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1);

        if (playerBox.intersects(blockBox)) {
            flagIfNeeded(player, state, now, "self_intersect");
        }
    }

    private void flagIfNeeded(WindfallPlayer player, PlacementState state, long now, String reason) {
        if (now - state.lastFlagTime < 3000) return;
        state.lastFlagTime = now;
        increaseBuffer(player, 1.5);
        flag(player);
        resetBuffer(player);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(UUID uuid) {
        playerStates.remove(uuid);
    }
}
