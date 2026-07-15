package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.player.WindfallPlayer;

public class WorldChange {
    private final int blockX, blockY, blockZ;
    private final int oldBlockId, newBlockId;
    private final long timestamp;
    private final ChangeType type;

    public enum ChangeType { BLOCK_PLACE, BLOCK_BREAK, PISTON_PUSH, LIQUID_FLOW }

    public WorldChange(int x, int y, int z, int oldId, int newId, ChangeType type) {
        this.blockX = x; this.blockY = y; this.blockZ = z;
        this.oldBlockId = oldId; this.newBlockId = newId;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public int getOldBlockId() { return oldBlockId; }
    public int getNewBlockId() { return newBlockId; }
    public long getTimestamp() { return timestamp; }
    public ChangeType getType() { return type; }

    public void apply(WindfallPlayer player) {
        // Apply the world change to the compensated world state
    }
}
