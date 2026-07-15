package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Criticals A", stableKey="windfall.combat.criticals", decay=0.02, setbackVl=20)
public class CriticalsCheck extends Check implements PacketCheck {
    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;
        final boolean[] isAttack = {false};
        p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
            @Override public void interact(net.minecraft.util.Hand hand) {}
            @Override public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d pos) {}
            @Override public void attack() { isAttack[0] = true; }
        });
        if (!isAttack[0]) return;
        if (player.isOnGround() && !player.isSneaking() && player.getVerticalSpeed() < 0.001) {
            double deltaY = Math.abs(player.getY() - player.getLastY());
            if (deltaY < 0.001 && deltaY > 0) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
}
