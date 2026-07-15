package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Backtrack A", stableKey="windfall.combat.backtrack", decay=0.02, setbackVl=25)
public class BacktrackCheck extends Check implements PacketCheck {
    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.InteractEntityC2SPacket)) return;
        int ping = player.getTransactionPing();
        if (ping > 200) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) { flag(player); resetBuffer(player); }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
}
