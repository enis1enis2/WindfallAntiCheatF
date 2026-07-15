package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="AirLiquidBreak A", stableKey="windfall.movement.airliquidbreak", decay=0.02, setbackVl=20)
public class AirLiquidBreakCheck extends Check implements PacketCheck {

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
