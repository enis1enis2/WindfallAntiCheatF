package io.windfall.anticheat.core.severity;

import io.windfall.anticheat.core.config.WindfallConfig;
import io.windfall.anticheat.core.player.WindfallPlayer;

public class SeverityManager {
    private final boolean enabled;
    private final int moderateVl, highVl, extremeVl;
    private final double moderateMultiplier, highMultiplier, extremeMultiplier;

    private SeverityManager(boolean enabled, int mod, int high, int extreme,
                            double modM, double highM, double extremeM) {
        this.enabled = enabled;
        this.moderateVl = mod; this.highVl = high; this.extremeVl = extreme;
        this.moderateMultiplier = modM; this.highMultiplier = highM; this.extremeMultiplier = extremeM;
    }

    public static SeverityManager fromConfig(WindfallConfig config) {
        return new SeverityManager(
            config.isSeverityEnabled(),
            config.getSeverityModerateVl(), config.getSeverityHighVl(), config.getSeverityExtremeVl(),
            config.getSeverityModerateMultiplier(), config.getSeverityHighMultiplier(), config.getSeverityExtremeMultiplier()
        );
    }

    public int getScaledVlIncrement(WindfallPlayer player) {
        if (!enabled) return 1;
        int total = player.getTotalViolationLevel();
        if (total >= extremeVl) return (int) Math.ceil(extremeMultiplier);
        if (total >= highVl) return (int) Math.ceil(highMultiplier);
        if (total >= moderateVl) return (int) Math.ceil(moderateMultiplier);
        return 1;
    }
}
