package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.util.MathUtil;

@CheckData(name="Aim A", stableKey="windfall.combat.aim", decay=0.01, setbackVl=15, relaxMultiplier=0.7)
public class AimCheck extends Check implements PacketCheck {
    private static final double MIN_DELTA_THRESHOLD = 0.015;
    private float lastYaw, lastPitch;

    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {}
    @Override public void onPacketSend(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket)) return;
        net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket p = (net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket) packet;
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        float deltaYaw = Math.abs(yaw - lastYaw);
        float deltaPitch = Math.abs(pitch - lastPitch);
        lastYaw = yaw;
        lastPitch = pitch;
        double deltaAngle = MathUtil.getAngleDelta(yaw, pitch, lastYaw, lastPitch);
        if (deltaAngle < MIN_DELTA_THRESHOLD) return;
        if (player.getLastX() == player.getX() && player.getLastY() == player.getY() && player.getLastZ() == player.getZ()) return;
        double sensitivity = player.getTransactionPing() / 50.0;
        double maxAngle = 40.0 + (sensitivity * 5.0);
        flagIfAboveThreshold(player, deltaAngle, maxAngle);
    }
}
