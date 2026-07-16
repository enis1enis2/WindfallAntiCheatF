package io.windfall.anticheat.core.util;

public final class MathUtil {

    private MathUtil() {}

    public static double clamp(double val, double min, double max) {
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double square(double val) {
        return val * val;
    }

    public static double horizontalDistance(double x1, double z1, double x2, double z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static double verticalDistance(double y1, double y2) {
        return Math.abs(y1 - y2);
    }

    public static double distance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double[] getDirection(float yaw) {
        double rad = Math.toRadians(yaw);
        double x = -Math.sin(rad);
        double z = Math.cos(rad);
        return new double[]{x, z};
    }

    public static double wrapDegrees(double deg) {
        deg %= 360.0;
        if (deg > 180.0) {
            deg -= 360.0;
        } else if (deg < -180.0) {
            deg += 360.0;
        }
        return deg;
    }

    public static int gcd(int a, int b) {
        if (b == 0) return Math.abs(a);
        return gcd(b, a % b);
    }

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

    public static float normalizeYaw(float yaw) {
        yaw = yaw % 360;
        if (yaw < -180) yaw += 360;
        if (yaw > 180) yaw -= 360;
        return yaw;
    }
}
