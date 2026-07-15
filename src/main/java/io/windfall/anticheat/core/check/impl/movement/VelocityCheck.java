package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Velocity A", stableKey="windfall.movement.velocity", decay=0.01, setbackVl=20)
public class VelocityCheck extends Check implements PacketCheck {

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)) return;

        if (!player.isVelocityReceived()) return;

        double dx = player.getVelocityX();
        double dy = player.getVelocityY();
        double dz = player.getVelocityZ();

        if (dy > 0 && Math.abs(player.getDeltaY()) < 0.01) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }

        player.setVelocityReceived(false);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
