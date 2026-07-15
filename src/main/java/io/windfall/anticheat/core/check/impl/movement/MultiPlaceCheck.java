package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="MultiPlace A", stableKey="windfall.movement.multiplace", decay=0.02, setbackVl=20)
public class MultiPlaceCheck extends Check implements PacketCheck {

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
