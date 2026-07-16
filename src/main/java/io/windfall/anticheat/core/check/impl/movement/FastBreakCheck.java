package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="FastBreak A", stableKey="windfall.movement.fastbreak", decay=0.02, setbackVl=20)
public class FastBreakCheck extends Check implements PacketCheck {

    private static final double FLAG_THRESHOLD = 3.0;
    private static final double FAST_BREAK_RATIO = 0.85;
    private static final double BUFFER_INCREMENT = 1.0;

    private final Map<UUID, BreakState> playerStates = new ConcurrentHashMap<>();

    private static final class BreakState {
        long startTimestamp;
        String blockId;
    }

    private static double getVanillaBreakTime(String blockId) {
        switch (blockId) {
            case "minecraft:obsidian": return 50.0;
            case "minecraft:crying_obsidian": return 50.0;
            case "minecraft:enchanting_table":
            case "minecraft:anvil":
            case "minecraft:chipped_anvil":
            case "minecraft:damaged_anvil": return 5.0;
            case "minecraft:diamond_block":
            case "minecraft:diamond_ore":
            case "minecraft:emerald_block":
            case "minecraft:emerald_ore": return 5.0;
            case "minecraft:iron_block":
            case "minecraft:iron_ore":
            case "minecraft:deepslate_iron_ore": return 3.0;
            case "minecraft:gold_block":
            case "minecraft:gold_ore":
            case "minecraft:deepslate_gold_ore": return 3.0;
            case "minecraft:stone":
            case "minecraft:cobblestone":
            case "minecraft:stone_bricks":
            case "minecraft:deepslate":
            case "minecraft:cobbled_deepslate": return 1.5;
            case "minecraft:glass":
            case "minecraft:sea_lantern":
            case "minecraft:glowstone": return 0.3;
            case "minecraft:oak_planks":
            case "minecraft:spruce_planks":
            case "minecraft:birch_planks":
            case "minecraft:jungle_planks":
            case "minecraft:acacia_planks":
            case "minecraft:dark_oak_planks":
            case "minecraft:cherry_planks":
            case "minecraft:mangrove_planks":
            case "minecraft:bamboo_planks": return 2.0;
            case "minecraft:end_stone": return 3.0;
            case "minecraft:netherrack": return 0.4;
            case "minecraft:soul_sand":
            case "minecraft:soul_soil": return 1.0;
            case "minecraft:bedrock":
            case "minecraft:barrier":
            case "minecraft:command_block":
            case "minecraft:structure_block": return Double.MAX_VALUE;
            default: return 1.0;
        }
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        PlayerActionC2SPacket.Action actionType = action.getAction();
        BlockPos blockPos = action.getPos();
        BreakState state = playerStates.computeIfAbsent(player.getUuid(), k -> new BreakState());

        if (actionType == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            String blockId = net.minecraft.registry.Registries.BLOCK.getId(
                    player.getServerPlayer().getWorld().getBlockState(blockPos).getBlock()).toString();
            state.startTimestamp = System.nanoTime();
            state.blockId = blockId;
        } else if (actionType == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
            if (state.startTimestamp == 0 || state.blockId == null) return;

            double elapsedSeconds = (System.nanoTime() - state.startTimestamp) / 1_000_000_000.0;
            double vanillaTime = getVanillaBreakTime(state.blockId);

            if (vanillaTime == Double.MAX_VALUE) return;

            double adjustedVanilla = vanillaTime * FAST_BREAK_RATIO;

            if (elapsedSeconds < adjustedVanilla && elapsedSeconds > 0.0) {
                increaseBuffer(player, BUFFER_INCREMENT);
                if (getBuffer(player) > FLAG_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.2);
            }

            state.startTimestamp = 0;
            state.blockId = null;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(UUID uuid) {
        playerStates.remove(uuid);
    }
}
