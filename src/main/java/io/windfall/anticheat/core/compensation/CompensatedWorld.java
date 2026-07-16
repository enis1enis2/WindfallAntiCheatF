package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.physics.BoundingBox;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.util.MaterialUtils;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CompensatedWorld {

    private final Map<Long, Block> blockChanges = new ConcurrentHashMap<>();
    private final World world;

    public CompensatedWorld() {
        this.world = null;
    }

    public CompensatedWorld(World world) {
        this.world = world;
    }

    public void onBlockChange(int x, int y, int z) {
        onBlockChange(x, y, z, net.minecraft.block.Blocks.AIR);
    }

    public void onBlockChange(int x, int y, int z, Block block) {
        long key = blockKey(x, y, z);
        blockChanges.put(key, block);
    }

    public Block getBlock(int x, int y, int z) {
        long key = blockKey(x, y, z);
        Block cached = blockChanges.get(key);
        if (cached != null) return cached;

        if (world != null) {
            try {
                return world.getBlockState(new BlockPos(x, y, z)).getBlock();
            } catch (Exception e) {
                return net.minecraft.block.Blocks.AIR;
            }
        }
        return net.minecraft.block.Blocks.AIR;
    }

    public double getBlockFriction(int x, int y, int z) {
        Block block = getBlock(x, y - 1, z);
        if (block == null) block = getBlock(x, y, z);
        return PhysicsConstants.getBlockFriction(block);
    }

    public boolean isOnClimbable(int x, int y, int z) {
        Block block = getBlock(x, y, z);
        return block != null && MaterialUtils.isClimbable(block);
    }

    public boolean isInWater(int x, int y, int z) {
        Block block = getBlock(x, y, z);
        return block != null && MaterialUtils.isWater(block);
    }

    public boolean isInLava(int x, int y, int z) {
        Block block = getBlock(x, y, z);
        return block != null && MaterialUtils.isLava(block);
    }

    public boolean isOnSlime(int x, int y, int z) {
        Block below = getBlock(x, y - 1, z);
        if (below != null && MaterialUtils.isSlime(below)) return true;
        Block at = getBlock(x, y, z);
        return at != null && MaterialUtils.isSlime(at);
    }

    public boolean isOnHoney(int x, int y, int z) {
        Block below = getBlock(x, y - 1, z);
        if (below != null && MaterialUtils.isHoney(below)) return true;
        Block at = getBlock(x, y, z);
        return at != null && MaterialUtils.isHoney(at);
    }

    public boolean isOnWeb(int x, int y, int z) {
        Block block = getBlock(x, y, z);
        return block != null && MaterialUtils.isWeb(block);
    }

    public boolean isOnSoulSand(int x, int y, int z) {
        Block block = getBlock(x, y - 1, z);
        return block != null && MaterialUtils.isSoulSand(block);
    }

    public boolean isOnIce(int x, int y, int z) {
        Block block = getBlock(x, y - 1, z);
        return block != null && MaterialUtils.isIce(block);
    }

    public boolean isBubbleColumn(int x, int y, int z) {
        Block block = getBlock(x, y, z);
        return block != null && MaterialUtils.isBubbleColumn(block);
    }

    public boolean isPowderSnow(int x, int y, int z) {
        Block block = getBlock(x, y, z);
        return block != null && MaterialUtils.isPowderSnow(block);
    }

    public List<BoundingBox> getCollisionBoxes(BoundingBox box, int protocolVersion) {
        List<BoundingBox> boxes = new ArrayList<>();
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX);
        int minY = (int) Math.floor(box.minY);
        int maxY = (int) Math.floor(box.maxY);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = getBlock(x, y, z);
                    if (block != null && MaterialUtils.isSolid(block)) {
                        boxes.add(new BoundingBox(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }
        return boxes;
    }

    public World getWorld() {
        return world;
    }

    private static long blockKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (long) (z & 0x3FFFFFF);
    }

    public void clear() {
        blockChanges.clear();
    }
}
