package io.windfall.anticheat.core.util;

public final class MathUtil {
    private MathUtil() {}
    public static double getAngleDelta(float yaw1, float pitch1, float yaw2, float pitch2) {
        float deltaYaw = yaw1 - yaw2;
        float deltaPitch = pitch1 - pitch2;
        return Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
    }
    public static double getAngleDelta(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    public static double wrapAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }
    public static double getDirectionalSpeed(double dx, double dz) {
        return Math.sqrt(dx * dx + dz * dz);
    }
    public static double getDistanceXZ(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }
    public static double getDistance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    public static float normalizeYaw(float yaw) {
        yaw = yaw % 360;
        if (yaw < -180) yaw += 360;
        if (yaw > 180) yaw -= 360;
        return yaw;
    }
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
