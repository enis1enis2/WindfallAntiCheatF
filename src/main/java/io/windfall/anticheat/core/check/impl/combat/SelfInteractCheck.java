package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.check.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name = "Self Interact A", stableKey = "windfall.combat.selfinteract", decay = 0.0, setbackVl = 5)
public class SelfInteractCheck extends Check implements PacketCheck {

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;

        final boolean[] isAttack = {false};
        final int[] targetIdArr = {-1};
        p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
            public void interact(net.minecraft.util.Hand hand) {}
            public void attack() {
                isAttack[0] = true;
                try {
                    net.minecraft.entity.Entity e = p.getEntity(player.getServerPlayer().getServerWorld());
                    if (e != null) targetIdArr[0] = e.getId();
                } catch (Exception ignored) {}
            }
            public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {}
        });
        if (!isAttack[0]) return;

        int targetId = targetIdArr[0];
        int selfId = player.getServerPlayer().getId();

        if (targetId == selfId) {
            flag(player);
            kickPlayer(player, "[Windfall] Self-interaction detected");
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private void kickPlayer(WindfallPlayer player, String reason) {
        net.minecraft.server.network.ServerPlayerEntity sp = player.getServerPlayer();
        if (sp != null && sp.networkHandler != null) {
            sp.networkHandler.disconnect(net.minecraft.text.Text.literal("[Windfall] " + reason));
        }
    }
}
