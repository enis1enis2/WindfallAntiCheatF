package io.windfall.anticheat.core.physics;

import io.windfall.anticheat.core.player.WindfallPlayer;

public final class PredictionEngine {

    private static final double GRAVITY_SLOW_FALLING = 0.01;
    private static final double WATER_GRAVITY_OFFSET = 0.02;
    private static final double LAVA_GRAVITY_OFFSET = 0.02;
    private static final double LEVITATION_STRENGTH = 0.05;
    private static final double WATER_DRAG_VERTICAL = 0.8;
    private static final double LAVA_DRAG_VERTICAL = 0.5;
    private static final double HONEY_MAX_DELTA_Y = -0.5;
    private static final double CLIMB_MAX_DELTA_Y = 0.15;
    private static final double GROUND_ACCEL_FACTOR = 0.16277136;
    private static final double AIR_ACCEL_FACTOR = 0.026;

    private PredictionEngine() {}

    public static double calculateHorizontalSpeed(double deltaX, double deltaZ) {
        return Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
    }

    public static double calculateBaseSpeed(boolean sprinting, boolean sneaking,
                                             double speedMultiplier, double slownessMultiplier) {
        double speed = PhysicsConstants.PLAYER_WALK_SPEED;
        if (sprinting) speed *= PhysicsConstants.PLAYER_SPRINT_MULTIPLIER;
        if (sneaking) speed *= PhysicsConstants.PLAYER_CROUCH_MULTIPLIER;
        speed *= speedMultiplier;
        speed *= slownessMultiplier;
        return speed;
    }

    public static double calculateMaxHorizontalSpeed(double baseSpeed, double lastHorizontalSpeed,
                                                      boolean onGround, boolean climbing,
                                                      boolean swimming, int protocol) {
        boolean inWeb = climbing && !swimming;
        double friction;

        if (onGround) {
            friction = PhysicsConstants.GROUND_FRICTION;
        } else {
            friction = PhysicsConstants.AIR_DRAG;
        }
        if (inWeb) friction = Math.min(friction, PhysicsConstants.WEB_FRICTION);

        double maxAccel;
        if (onGround) {
            double accelFactor = GROUND_ACCEL_FACTOR / (friction * friction * friction);
            maxAccel = baseSpeed * accelFactor;
        } else {
            maxAccel = baseSpeed * AIR_ACCEL_FACTOR;
        }

        if (swimming) maxAccel *= 0.9;

        return lastHorizontalSpeed * friction + maxAccel;
    }

    public static double calculateMaxHorizontalSpeed(double baseSpeed, double lastHorizontalSpeed,
                                                      boolean onGround, boolean climbing,
                                                      boolean swimming, int protocol, double deltaY) {
        boolean inWeb = climbing && !swimming;
        double friction;

        if (onGround) {
            friction = PhysicsConstants.GROUND_FRICTION;
        } else {
            friction = PhysicsConstants.AIR_DRAG;
        }
        if (inWeb) friction = Math.min(friction, PhysicsConstants.WEB_FRICTION);

        double maxAccel;
        if (onGround) {
            double accelFactor = GROUND_ACCEL_FACTOR / (friction * friction * friction);
            maxAccel = baseSpeed * accelFactor;
        } else {
            maxAccel = baseSpeed * AIR_ACCEL_FACTOR;
        }

        if (swimming) maxAccel *= 0.9;

        double maxSpeed = lastHorizontalSpeed * friction + maxAccel;

        if (swimming && protocol >= 393) {
            double swimBoost = 0.01 * Math.max(0, deltaY);
            maxSpeed += swimBoost;
        }
        return maxSpeed;
    }

    public static double predictDeltaY(double currentExpectedDeltaY, boolean inWater, boolean inLava,
                                        boolean climbing, boolean onHoney, boolean hasSlowFalling,
                                        boolean hasLevitation, double levitationAmplifier,
                                        boolean isFallFlying, boolean hasRiptide) {
        double gravity = hasSlowFalling ? GRAVITY_SLOW_FALLING : PhysicsConstants.GRAVITY;

        if (inWater) {
            return currentExpectedDeltaY * WATER_DRAG_VERTICAL - WATER_GRAVITY_OFFSET;
        } else if (inLava) {
            return currentExpectedDeltaY * LAVA_DRAG_VERTICAL - LAVA_GRAVITY_OFFSET;
        } else if (climbing) {
            double result = currentExpectedDeltaY;
            if (currentExpectedDeltaY > CLIMB_MAX_DELTA_Y) result = CLIMB_MAX_DELTA_Y;
            return result;
        } else if (onHoney) {
            return Math.max(currentExpectedDeltaY, HONEY_MAX_DELTA_Y);
        } else if (isFallFlying || hasRiptide) {
            return currentExpectedDeltaY;
        } else if (hasLevitation) {
            return currentExpectedDeltaY + LEVITATION_STRENGTH * levitationAmplifier;
        } else {
            return (currentExpectedDeltaY - gravity) * PhysicsConstants.AIR_DRAG;
        }
    }

    public static boolean checkInWater(WindfallPlayer player) {
        return player.isCachedInWater();
    }

    public static boolean checkInLava(WindfallPlayer player) {
        return player.isCachedInLava();
    }

    public static boolean checkOnHoney(WindfallPlayer player) {
        return player.isCachedOnHoney();
    }

    public static double getSpeedPotionMultiplier(WindfallPlayer player) {
        return player.getCachedSpeedMultiplier();
    }

    public static double getSlownessPotionMultiplier(WindfallPlayer player) {
        return player.getCachedSlownessMultiplier();
    }

    public static boolean checkSlowFalling(WindfallPlayer player) {
        return player.isCachedHasSlowFalling();
    }

    public static boolean checkLevitation(WindfallPlayer player) {
        return player.isCachedHasLevitation();
    }

    public static double getLevitationAmplifier(WindfallPlayer player) {
        return player.getCachedLevitationAmplifier();
    }

    public static boolean checkRiptiding(WindfallPlayer player) {
        return player.isCachedHasRiptide();
    }

    public static boolean checkFallFlying(WindfallPlayer player) {
        return player.isCachedIsFallFlying();
    }
}
