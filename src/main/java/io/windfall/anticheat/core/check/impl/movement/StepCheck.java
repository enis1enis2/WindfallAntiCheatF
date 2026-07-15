package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Step A", stableKey="windfall.movement.step", decay=0.02, setbackVl=20)
public class StepCheck extends Check implements PacketCheck {

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)) return;

        double delta = Math.abs(player.getY() - player.getLastY());

        if (delta > 0.6 && player.isOnGround()) {
            flagIfAboveThreshold(player, delta, 0.6);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
