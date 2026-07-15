package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Motion A", stableKey="windfall.movement.motion", decay=0.02, setbackVl=20)
public class MotionCheck extends Check implements PacketCheck {

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
