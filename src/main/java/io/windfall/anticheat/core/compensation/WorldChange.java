package io.windfall.anticheat.core.compensation;

public final class WorldChange {

    public enum Type {
        BLOCK_BREAK,
        BLOCK_PLACE,
        BLOCK_SHIFT,
        VELOCITY,
        POTION_EFFECT
    }

    private final Type type;
    private final int tick;
    private final long timestamp;

    private final int blockX, blockY, blockZ;
    private final int oldX, oldY, oldZ;

    private final double velocityX, velocityY, velocityZ;
    private final double gravityMod;
    private final double airDragMod;

    private WorldChange(Type type, int tick,
                        int blockX, int blockY, int blockZ,
                        int oldX, int oldY, int oldZ,
                        double velocityX, double velocityY, double velocityZ,
                        double gravityMod, double airDragMod) {
        this.type = type;
        this.tick = tick;
        this.timestamp = System.currentTimeMillis();
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.oldX = oldX;
        this.oldY = oldY;
        this.oldZ = oldZ;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.gravityMod = gravityMod;
        this.airDragMod = airDragMod;
    }

    public static WorldChange blockBreak(int tick, int x, int y, int z) {
        return new WorldChange(Type.BLOCK_BREAK, tick, x, y, z, 0, 0, 0, 0, 0, 0, 1.0, 1.0);
    }

    public static WorldChange blockPlace(int tick, int x, int y, int z) {
        return new WorldChange(Type.BLOCK_PLACE, tick, x, y, z, 0, 0, 0, 0, 0, 0, 1.0, 1.0);
    }

    public static WorldChange blockShift(int tick, int oldX, int oldY, int oldZ,
                                         int newX, int newY, int newZ) {
        return new WorldChange(Type.BLOCK_SHIFT, tick, newX, newY, newZ, oldX, oldY, oldZ, 0, 0, 0, 1.0, 1.0);
    }

    public static WorldChange velocity(int tick, double vx, double vy, double vz) {
        return new WorldChange(Type.VELOCITY, tick, 0, 0, 0, 0, 0, 0, vx, vy, vz, 1.0, 1.0);
    }

    public static WorldChange potionEffect(int tick, double gravityMod, double airDragMod) {
        return new WorldChange(Type.POTION_EFFECT, tick, 0, 0, 0, 0, 0, 0, 0, 0, 0, gravityMod, airDragMod);
    }

    public Type getType() { return type; }
    public int getTick() { return tick; }
    public long getTimestamp() { return timestamp; }
    public int getBlockX() { return blockX; }
    public int getBlockY() { return blockY; }
    public int getBlockZ() { return blockZ; }
    public int getOldX() { return oldX; }
    public int getOldY() { return oldY; }
    public int getOldZ() { return oldZ; }
    public double getVelocityX() { return velocityX; }
    public double getVelocityY() { return velocityY; }
    public double getVelocityZ() { return velocityZ; }
    public double getGravityMod() { return gravityMod; }
    public double getAirDragMod() { return airDragMod; }
}
