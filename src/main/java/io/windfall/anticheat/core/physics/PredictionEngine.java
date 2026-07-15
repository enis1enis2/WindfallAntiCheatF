package io.windfall.anticheat.core.physics;

import io.windfall.anticheat.core.player.WindfallPlayer;

public class PredictionEngine {
    public PredictionEngine() {}

    public PredictionResult predict(WindfallPlayer player, PredictionContext ctx, int serverProtocol) {
        double startX = ctx.getPosX();
        double startY = ctx.getPosY();
        double startZ = ctx.getPosZ();
        double mx = ctx.getMotionX();
        double my = ctx.getMotionY();
        double mz = ctx.getMotionZ();

        double speed = ctx.isSprinting() ? PhysicsConstants.SPRINT_SPEED : PhysicsConstants.WALK_SPEED;
        if (ctx.isSneaking()) speed = PhysicsConstants.SNEAK_SPEED;
        if (ctx.isSwimming()) speed = PhysicsConstants.SWIM_SPEED;

        if (!ctx.isOnGround()) {
            my += PhysicsConstants.GRAVITY;
            if (ctx.isGliding()) {
                my *= 0.98;
                my = Math.max(my, -0.5);
            } else {
                my *= PhysicsConstants.AIR_DRAG;
            }
        }

        mx *= PhysicsConstants.AIR_DRAG;
        mz *= PhysicsConstants.AIR_DRAG;

        double predictedX = startX + mx;
        double predictedY = startY + my;
        double predictedZ = startZ + mz;

        double dx = player.getX() - predictedX;
        double dy = player.getY() - predictedY;
        double dz = player.getZ() - predictedZ;
        double totalDelta = Math.sqrt(dx * dx + dy * dy + dz * dz);

        boolean valid = totalDelta < 0.1;

        return new PredictionResult(predictedX, predictedY, predictedZ, totalDelta, valid);
    }

    public static class PredictionResult {
        private final double predictedX, predictedY, predictedZ;
        private final double totalDelta;
        private final boolean valid;

        public PredictionResult(double px, double py, double pz, double delta, boolean valid) {
            this.predictedX = px; this.predictedY = py; this.predictedZ = pz;
            this.totalDelta = delta; this.valid = valid;
        }

        public double getPredictedX() { return predictedX; }
        public double getPredictedY() { return predictedY; }
        public double getPredictedZ() { return predictedZ; }
        public double getTotalDelta() { return totalDelta; }
        public boolean isValid() { return valid; }
    }
}
