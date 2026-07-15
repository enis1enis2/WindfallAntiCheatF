package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="SwordBlock A", stableKey="windfall.combat.swordblock", decay=0.02, setbackVl=20, compat={CompatFlag.VERSION_LEGACY})
public class SwordBlockCheck extends Check implements PacketCheck {
    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {}
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
}
