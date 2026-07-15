package io.windfall.anticheat.core.check.impl.inventory;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Inventory A", stableKey="windfall.inventory.move", decay=0.02, setbackVl=20)
public class InventoryCheck extends Check implements PacketCheck {
    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {}
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
}
