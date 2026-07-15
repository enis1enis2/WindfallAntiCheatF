package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="GroundSpoof A", stableKey="windfall.movement.groundspoof", decay=0.02, setbackVl=20)
public class GroundSpoofCheck extends Check implements PacketCheck {

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)) return;

        double deltaY = Math.abs(player.getY() - player.getLastY());

        if (player.isOnGround() && deltaY > 0.5) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
