package io.windfall.anticheat.core.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MaterialUtils {

    private MaterialUtils() {}

    private static final Map<Block, Boolean> COLLISION_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> FLUID_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> CLIMBABLE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> ICE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> SLIME_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> HONEY_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> WEB_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> SOUL_SAND_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> BUBBLE_COLUMN_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> POWDER_SNOW_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> REPLACEABLE_CACHE = new ConcurrentHashMap<>();
    private static final Map<Block, Boolean> NON_FULL_SHAPE_CACHE = new ConcurrentHashMap<>();

    private static final Set<Block> CLIMBABLE_BLOCKS = Set.of(
        Blocks.LADDER, Blocks.VINE, Blocks.TWISTING_VINES, Blocks.TWISTING_VINES_PLANT,
        Blocks.WEEPING_VINES, Blocks.WEEPING_VINES_PLANT, Blocks.SOUL_SAND, Blocks.SOUL_SOIL
    );

    public static boolean isFluid(Block block) {
        if (block == null || block == Blocks.AIR) return false;
        return FLUID_CACHE.computeIfAbsent(block, b -> {
            try {
                return b.getDefaultState().getFluidState().isOf(Fluids.WATER)
                    || b.getDefaultState().getFluidState().isOf(Fluids.LAVA);
            } catch (Exception e) {
                return false;
            }
        });
    }

    public static boolean isWater(Block block) {
        if (block == null) return false;
        try {
            return block.getDefaultState().getFluidState().isOf(Fluids.WATER);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isLava(Block block) {
        if (block == null) return false;
        try {
            return block.getDefaultState().getFluidState().isOf(Fluids.LAVA);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean hasCollision(Block block) {
        if (block == null || block == Blocks.AIR) return false;
        return COLLISION_CACHE.computeIfAbsent(block, b -> {
            if (isFluid(b) || isAirLike(b)) return false;
            try {
                return b.getDefaultState().isSolid();
            } catch (Exception e) {
                return false;
            }
        });
    }

    public static boolean isClimbable(Block block) {
        if (block == null) return false;
        return CLIMBABLE_CACHE.computeIfAbsent(block, CLIMBABLE_BLOCKS::contains);
    }

    public static boolean isIce(Block block) {
        if (block == null) return false;
        return ICE_CACHE.computeIfAbsent(block, b -> {
            String id = Registries.BLOCK.getId(b).getPath();
            return id.contains("ice");
        });
    }

    public static boolean isSlime(Block block) {
        if (block == null) return false;
        return SLIME_CACHE.computeIfAbsent(block, b -> b == Blocks.SLIME_BLOCK || b == Blocks.HONEY_BLOCK);
    }

    public static boolean isHoney(Block block) {
        if (block == null) return false;
        return HONEY_CACHE.computeIfAbsent(block, b -> b == Blocks.HONEY_BLOCK);
    }

    public static boolean isWeb(Block block) {
        if (block == null) return false;
        return WEB_CACHE.computeIfAbsent(block, b -> b == Blocks.COBWEB);
    }

    public static boolean isSoulSand(Block block) {
        if (block == null) return false;
        return SOUL_SAND_CACHE.computeIfAbsent(block, b -> b == Blocks.SOUL_SAND || b == Blocks.SOUL_SOIL);
    }

    public static boolean isBubbleColumn(Block block) {
        if (block == null) return false;
        return BUBBLE_COLUMN_CACHE.computeIfAbsent(block, b -> b == Blocks.BUBBLE_COLUMN);
    }

    public static boolean isPowderSnow(Block block) {
        if (block == null) return false;
        return POWDER_SNOW_CACHE.computeIfAbsent(block, b -> b == Blocks.POWDER_SNOW);
    }

    public static boolean isReplaceable(Block block) {
        if (block == null || block == Blocks.AIR) return true;
        return REPLACEABLE_CACHE.computeIfAbsent(block, b -> {
            if (isFluid(b)) return true;
            try {
                return b == Blocks.AIR || !b.getDefaultState().isSolid();
            } catch (Exception e) {
                return isAirLike(b) || isFluid(b);
            }
        });
    }

    public static boolean isNonFullShape(Block block) {
        if (block == null || block == Blocks.AIR) return false;
        return NON_FULL_SHAPE_CACHE.computeIfAbsent(block, b -> {
            if (!hasCollision(b)) return false;
            String path = Registries.BLOCK.getId(b).getPath();
            return path.contains("stairs") || path.contains("slab")
                || path.contains("fence") || path.contains("wall")
                || path.contains("pane") || path.contains("door")
                || path.contains("trapdoor") || path.contains("pressure_plate")
                || path.contains("carpet") || path.contains("lantern")
                || path.contains("chain") || path.contains("bars")
                || path.contains("cauldron") || path.contains("composter")
                || path.contains("snow") || path.contains("lily_pad");
        });
    }

    public static boolean isFence(Block block) {
        if (block == null) return false;
        return Registries.BLOCK.getId(block).getPath().contains("fence");
    }

    public static boolean isWall(Block block) {
        if (block == null) return false;
        return Registries.BLOCK.getId(block).getPath().contains("wall");
    }

    public static boolean isStair(Block block) {
        if (block == null) return false;
        return Registries.BLOCK.getId(block).getPath().contains("stair");
    }

    public static boolean isSolid(Block block) {
        if (block == null) return false;
        try {
            return block.getDefaultState().isSolid();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAirLike(Block block) {
        if (block == null) return false;
        return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
    }

    private static boolean isAirLike(String name) {
        return name.equals("AIR") || name.equals("CAVE_AIR") || name.equals("VOID_AIR");
    }

    public static void clearCaches() {
        COLLISION_CACHE.clear();
        FLUID_CACHE.clear();
        CLIMBABLE_CACHE.clear();
        ICE_CACHE.clear();
        SLIME_CACHE.clear();
        HONEY_CACHE.clear();
        WEB_CACHE.clear();
        SOUL_SAND_CACHE.clear();
        BUBBLE_COLUMN_CACHE.clear();
        POWDER_SNOW_CACHE.clear();
        REPLACEABLE_CACHE.clear();
        NON_FULL_SHAPE_CACHE.clear();
    }
}
