package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Flight A", stableKey="windfall.movement.fly", decay=0.01, setbackVl=15)
public class FlightCheck extends Check implements PacketCheck {

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)) return;

        if (player.isOnGround() || player.isFlying() || player.isGliding()) return;

        double deltaY = player.getDeltaY();

        if (deltaY > 0 && !player.isOnGround()) {
            flagIfAboveThreshold(player, deltaY, 0.5);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
