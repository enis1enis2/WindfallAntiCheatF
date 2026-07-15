package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;

@CheckData(name="Sprint A", stableKey="windfall.packet.sprint", decay=0.02, setbackVl=20)
public class SprintCheck extends Check implements PacketCheck {
    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (packet instanceof net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket) {
            net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket p = (net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket) packet;
            // Detect sprint toggle
        }
    }
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}
}
