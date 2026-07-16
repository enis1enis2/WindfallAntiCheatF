package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.server.world.ServerWorld;

@CheckData(name="SelfInteract A", stableKey="windfall.combat.selfinteract", decay=0.02, setbackVl=20)
public class SelfInteractCheck extends Check implements PacketCheck {
    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;
        try {
            ServerWorld serverWorld = (ServerWorld) player.getServerPlayer().getWorld();
            net.minecraft.entity.Entity entity = p.getEntity(serverWorld);
            if (entity != null && entity.getId() == player.getServerPlayer().getId()) {
                flagWithSetback(player);
            }
        } catch (Exception e) {
            // Entity might not be in the same world
        }
    }
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
}
