package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Timer A", stableKey="windfall.movement.timer", decay=0.01, setbackVl=20)
public class TimerCheck extends Check implements PacketCheck {

    private int packetCount = 0;

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket) {
            packetCount++;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public void onTick(WindfallPlayer player) {
        if (packetCount > 25) {
            increaseBuffer(player, (packetCount - 25) / 5.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.2);
        }
        packetCount = 0;
    }
}
