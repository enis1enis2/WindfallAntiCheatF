package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="NoFall A", stableKey="windfall.movement.nofall", decay=0.02, setbackVl=20)
public class NoFallCheck extends Check implements PacketCheck {

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)) return;

        if (player.isOnGround() && !player.isLastOnGround() && player.getDeltaY() == 0 && player.getVerticalSpeed() < 0.001) {
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
