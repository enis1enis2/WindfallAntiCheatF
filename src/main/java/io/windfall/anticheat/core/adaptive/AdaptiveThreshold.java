package io.windfall.anticheat.core.adaptive;

import io.windfall.anticheat.core.config.WindfallConfig;

import java.util.ArrayDeque;

public class AdaptiveThreshold {

    private static final int DEFAULT_WINDOW_SIZE = 50;
    private static final double DEFAULT_TPS_THRESHOLD = 19.0;
    private static final double DEFAULT_SCALE_FACTOR = 0.02;
    private static final double DEFAULT_MAX_MULTIPLIER = 2.0;
    private static final double DEFAULT_SAFE_MODE_TPS = 12.0;

    private static final AdaptiveThreshold INSTANCE = new AdaptiveThreshold(DEFAULT_WINDOW_SIZE);

    private final ArrayDeque<Double> tpsSamples;
    private final int windowSize;

    private volatile boolean enabled;
    private volatile double tpsThreshold;
    private volatile double scaleFactor;
    private volatile double maxToleranceMultiplier;
    private volatile double safeModeTps;

    private volatile double currentTps = 20.0;
    private volatile double currentMultiplier = 1.0;
    private volatile boolean safeMode = false;

    public static AdaptiveThreshold getInstance() {
        return INSTANCE;
    }

    AdaptiveThreshold(int windowSize) {
        this.windowSize = windowSize;
        this.tpsSamples = new ArrayDeque<>(windowSize + 1);
        this.enabled = true;
        this.tpsThreshold = DEFAULT_TPS_THRESHOLD;
        this.scaleFactor = DEFAULT_SCALE_FACTOR;
        this.maxToleranceMultiplier = DEFAULT_MAX_MULTIPLIER;
        this.safeModeTps = DEFAULT_SAFE_MODE_TPS;
    }

    public void loadConfig(WindfallConfig config) {
        this.enabled = config.isAdaptiveEnabled();
        this.tpsThreshold = config.getAdaptiveTpsThreshold();
        this.scaleFactor = config.getAdaptiveScaleFactor();
        this.maxToleranceMultiplier = config.getAdaptiveMaxToleranceMultiplier();
        this.safeModeTps = config.getAdaptiveSafeModeThreshold();
    }

    public void onTick(long currentTickMs) {
        if (!enabled) return;

        double tickTps = 1000.0 / Math.max(currentTickMs, 1);

        synchronized (tpsSamples) {
            if (tpsSamples.size() >= windowSize) {
                tpsSamples.pollFirst();
            }
            tpsSamples.addLast(tickTps);
        }

        recalculate();
    }

    public void pushTps(double tps) {
        if (!enabled) return;

        synchronized (tpsSamples) {
            if (tpsSamples.size() >= windowSize) {
                tpsSamples.pollFirst();
            }
            tpsSamples.addLast(tps);
        }

        recalculate();
    }

    private void recalculate() {
        double avgTps;
        synchronized (tpsSamples) {
            if (tpsSamples.isEmpty()) {
                avgTps = 20.0;
            } else {
                double sum = 0;
                for (double s : tpsSamples) {
                    sum += s;
                }
                avgTps = sum / tpsSamples.size();
            }
        }

        this.currentTps = avgTps;

        if (avgTps < safeModeTps) {
            this.safeMode = true;
            this.currentMultiplier = maxToleranceMultiplier;
        } else if (avgTps < tpsThreshold) {
            this.safeMode = false;
            double deficiency = tpsThreshold - avgTps;
            double multiplier = 1.0 + (deficiency * scaleFactor);
            this.currentMultiplier = Math.min(multiplier, maxToleranceMultiplier);
        } else {
            this.safeMode = false;
            this.currentMultiplier = 1.0;
        }
    }

    public double applyTolerance(double baseThreshold) {
        if (!enabled) return baseThreshold;
        return baseThreshold * currentMultiplier;
    }

    public boolean isSafeMode() {
        return safeMode;
    }

    public double getToleranceMultiplier() {
        return enabled ? currentMultiplier : 1.0;
    }

    public double getCurrentTps() {
        return currentTps;
    }

    public double getCurrentMultiplier() {
        return currentMultiplier;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getTpsThreshold() {
        return tpsThreshold;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public double getMaxToleranceMultiplier() {
        return maxToleranceMultiplier;
    }

    public double getSafeModeTps() {
        return safeModeTps;
    }

    public void reset() {
        synchronized (tpsSamples) {
            tpsSamples.clear();
        }
        this.currentTps = 20.0;
        this.currentMultiplier = 1.0;
        this.safeMode = false;
    }
}
