package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="SelfInteract A", stableKey="windfall.combat.selfinteract", decay=0.02, setbackVl=20)
public class SelfInteractCheck extends Check implements PacketCheck {
    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;
        try {
            net.minecraft.entity.Entity entity = p.getEntity(player.getServerPlayer().getServerWorld());
            if (entity != null && entity.getId() == player.getServerPlayer().getId()) {
                flagWithSetback(player);
            }
        } catch (Exception e) {
            // Entity might not be in the same world
        }
    }
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
}
