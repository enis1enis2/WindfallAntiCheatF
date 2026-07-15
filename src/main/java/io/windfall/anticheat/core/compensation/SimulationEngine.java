package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.core.player.WindfallPlayer;

public class SimulationEngine {
    private final PingPongManager pingPongManager;
    private final LatencyCompensator latencyCompensator;

    public SimulationEngine(PingPongManager ppm, LatencyCompensator lc) {
        this.pingPongManager = ppm;
        this.latencyCompensator = lc;
    }

    public SimulationResult simulate(WindfallPlayer player, double targetX, double targetY, double targetZ, int ticksAhead) {
        return new SimulationResult(targetX, targetY, targetZ, true);
    }

    public static class SimulationResult {
        private final double x, y, z;
        private final boolean reachable;
        public SimulationResult(double x, double y, double z, boolean reachable) {
            this.x = x; this.y = y; this.z = z; this.reachable = reachable;
        }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public boolean isReachable() { return reachable; }
    }
}
