package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="InvalidBreak A", stableKey="windfall.movement.invalidbreak", decay=0.02, setbackVl=20)
public class InvalidBreakCheck extends Check implements PacketCheck {

    private static final String[] INDESTRUCTIBLE = {
            "minecraft:bedrock",
            "minecraft:barrier",
            "minecraft:end_portal",
            "minecraft:end_portal_frame",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:structure_block",
            "minecraft:structure_void",
            "minecraft:jigsaw",
            "minecraft:moving_piston"
    };

    private final Map<UUID, Long> lastFlagTime = new ConcurrentHashMap<>();

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        if (action.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return;

        BlockPos blockPos = action.getPos();

        if (player.getServerPlayer() == null || player.getServerPlayer().getWorld() == null) return;
        BlockState blockState = player.getServerPlayer().getWorld().getBlockState(blockPos);

        boolean isAir = blockState.isOf(Blocks.AIR)
                || blockState.isOf(Blocks.CAVE_AIR)
                || blockState.isOf(Blocks.VOID_AIR);

        if (isAir) {
            flagViolation(player, "air");
            return;
        }

        String blockId = Registries.BLOCK.getId(blockState.getBlock()).toString();
        for (String indestructible : INDESTRUCTIBLE) {
            if (blockId.equals(indestructible)) {
                flagViolation(player, "indestructible:" + blockId);
                return;
            }
        }
    }

    private void flagViolation(WindfallPlayer player, String reason) {
        long now = System.currentTimeMillis();
        Long last = lastFlagTime.get(player.getUuid());
        if (last != null && now - last < 2000) return;
        lastFlagTime.put(player.getUuid(), now);
        increaseBuffer(player, 2.0);
        flag(player);
        resetBuffer(player);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(UUID uuid) {
        lastFlagTime.remove(uuid);
    }
}
