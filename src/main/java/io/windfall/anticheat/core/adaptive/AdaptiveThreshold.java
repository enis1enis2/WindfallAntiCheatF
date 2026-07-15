package io.windfall.anticheat.core.adaptive;

import io.windfall.anticheat.core.config.WindfallConfig;

public class AdaptiveThreshold {
    private static final AdaptiveThreshold INSTANCE = new AdaptiveThreshold();
    private boolean enabled = true;
    private double tpsThreshold = 19.0;
    private double scaleFactor = 0.02;
    private double maxToleranceMultiplier = 2.0;
    private double safeModeThreshold = 12.0;
    private double currentTps = 20.0;
    private double toleranceMultiplier = 1.0;

    private AdaptiveThreshold() {}
    public static AdaptiveThreshold getInstance() { return INSTANCE; }

    public void loadConfig(WindfallConfig config) {
        this.enabled = config.isAdaptiveEnabled();
        this.tpsThreshold = config.getAdaptiveTpsThreshold();
        this.scaleFactor = config.getAdaptiveScaleFactor();
        this.maxToleranceMultiplier = config.getAdaptiveMaxToleranceMultiplier();
        this.safeModeThreshold = config.getAdaptiveSafeModeThreshold();
    }

    public void onTick(long tickDurationNanos) {
        double tickMs = tickDurationNanos / 1_000_000.0;
        this.currentTps = Math.min(20.0, 1000.0 / tickMs);
        if (currentTps < safeModeThreshold) {
            toleranceMultiplier = maxToleranceMultiplier;
        } else if (currentTps < tpsThreshold) {
            double drop = tpsThreshold - currentTps;
            toleranceMultiplier = Math.min(maxToleranceMultiplier, 1.0 + (drop * scaleFactor));
        } else {
            toleranceMultiplier = 1.0;
        }
    }

    public double getToleranceMultiplier() { return enabled ? toleranceMultiplier : 1.0; }
    public double getCurrentTps() { return currentTps; }
    public boolean isEnabled() { return enabled; }
}
