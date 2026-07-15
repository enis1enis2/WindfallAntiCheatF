package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.util.math.BlockPos;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensatedWorld {
    private final WindfallPlayer player;
    private final Map<Long, WorldChange> blockChanges = new ConcurrentHashMap<>();

    public CompensatedWorld(WindfallPlayer player) {
        this.player = player;
    }

    public void addBlockChange(WorldChange change) {
        blockChanges.put(posToLong(change.getBlockX(), change.getBlockY(), change.getBlockZ()), change);
    }

    public WorldChange getBlockChange(int x, int y, int z) {
        return blockChanges.get(posToLong(x, y, z));
    }

    private long posToLong(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) | (((long) y & 0x3FFFFFF) << 26) | (((long) z & 0x3FFFFFF) << 52);
    }
}
