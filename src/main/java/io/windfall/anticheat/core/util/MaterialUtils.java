package io.windfall.anticheat.core.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;

import java.util.Set;

public final class MaterialUtils {
    private MaterialUtils() {}
    private static final Set<Block> CLIMBABLE = Set.of(Blocks.LADDER, Blocks.VINE, Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT, Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT, Blocks.SOUL_SAND, Blocks.SOUL_SOIL);

    public static boolean isWater(Block block) {
        return block.getDefaultState().getFluidState().isOf(net.minecraft.fluid.Fluids.WATER);
    }
    public static boolean isLava(Block block) {
        return block.getDefaultState().getFluidState().isOf(net.minecraft.fluid.Fluids.LAVA);
    }
    public static boolean isIce(Block block) {
        String id = Registries.BLOCK.getId(block).toString();
        return id.contains("ice");
    }
    public static boolean isClimbable(Block block) {
        return CLIMBABLE.contains(block);
    }
    public static boolean isSolid(Block block) {
        return block.getDefaultState().isSolid();
    }
    public static boolean isSlime(Block block) {
        return block == Blocks.SLIME_BLOCK || block == Blocks.HONEY_BLOCK;
    }
    public static boolean isSoulSand(Block block) {
        return block == Blocks.SOUL_SAND || block == Blocks.SOUL_SOIL;
    }
    public static boolean isHoney(Block block) {
        return block == Blocks.HONEY_BLOCK;
    }
    public static boolean isWeb(Block block) {
        return block == Blocks.COBWEB || block == Blocks.CAVE_VINES || block == Blocks.CAVE_VINES_PLANT;
    }
    public static boolean isFence(Block block) {
        return Registries.BLOCK.getId(block).getPath().contains("fence");
    }
    public static boolean isWall(Block block) {
        return Registries.BLOCK.getId(block).getPath().contains("wall");
    }
    public static boolean isStair(Block block) {
        return Registries.BLOCK.getId(block).getPath().contains("stair");
    }
    public static void clearCaches() {
    }
}
