package io.windfall.anticheat.core.physics;

import net.minecraft.util.shape.VoxelShapes;

public class BoundingBox {
    private final double minX, minY, minZ;
    private final double maxX, maxY, maxZ;

    public BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
    }

    public static BoundingBox fromPlayer(double x, double y, double z, double width, double height) {
        double half = width / 2.0;
        return new BoundingBox(x - half, y, z - half, x + half, y + height, z + half);
    }

    public BoundingBox offset(double dx, double dy, double dz) {
        return new BoundingBox(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    public boolean intersects(BoundingBox other) {
        return this.maxX > other.minX && this.minX < other.maxX
            && this.maxY > other.minY && this.minY < other.maxY
            && this.maxZ > other.minZ && this.minZ < other.maxZ;
    }

    public double getMinX() { return minX; }
    public double getMinY() { return minY; }
    public double getMinZ() { return minZ; }
    public double getMaxX() { return maxX; }
    public double getMaxY() { return maxY; }
    public double getMaxZ() { return maxZ; }
    public double getWidthX() { return maxX - minX; }
    public double getHeight() { return maxY - minY; }
    public double getCenterX() { return (minX + maxX) / 2.0; }
    public double getCenterY() { return (minY + maxY) / 2.0; }
    public double getCenterZ() { return (minZ + maxZ) / 2.0; }

    public double getDistanceTo(BoundingBox other) {
        double dx = Math.max(0, Math.max(this.minX - other.maxX, other.minX - this.maxX));
        double dy = Math.max(0, Math.max(this.minY - other.maxY, other.minY - this.maxY));
        double dz = Math.max(0, Math.max(this.minZ - other.maxZ, other.minZ - this.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public String toString() {
        return String.format("BB[%.2f,%.2f,%.2f -> %.2f,%.2f,%.2f]", minX, minY, minZ, maxX, maxY, maxZ);
    }
}
