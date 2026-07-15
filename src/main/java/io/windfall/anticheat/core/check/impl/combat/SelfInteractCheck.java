package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="SelfInteract A", stableKey="windfall.combat.selfinteract", decay=0.02, setbackVl=20)
public class SelfInteractCheck extends Check implements PacketCheck {
    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.InteractEntityC2SPacket)) return;
        net.minecraft.network.packet.c2s.play.InteractEntityC2SPacket p = (net.minecraft.network.packet.c2s.play.InteractEntityC2SPacket) packet;
        if (p.getEntityId() == player.getServerPlayer().getId()) {
            flagWithSetback(player);
        }
    }
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
}
