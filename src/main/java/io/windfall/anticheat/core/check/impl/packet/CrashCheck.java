package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Crash A", stableKey="windfall.packet.crash", decay=0.02, setbackVl=20)
public class CrashCheck extends Check implements PacketCheck {
    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {}
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
}
