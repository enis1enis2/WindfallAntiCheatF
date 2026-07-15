package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Speed A", stableKey="windfall.movement.speed", decay=0.01, setbackVl=15, compat={CompatFlag.VIAVERSION_SENSITIVE})
public class SpeedCheck extends Check implements PacketCheck {

    private static final double MAX_SPEED = 0.35;

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)) return;

        double speed = player.getHorizontalSpeed();

        boolean onIce = false; // check block under player
        double maxSpd = onIce ? MAX_SPEED * 2.5 : MAX_SPEED;

        flagIfAboveThreshold(player, speed, maxSpd);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
