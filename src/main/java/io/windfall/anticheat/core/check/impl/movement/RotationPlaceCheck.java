package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="RotationPlace A", stableKey="windfall.movement.rotationplace", decay=0.02, setbackVl=20)
public class RotationPlaceCheck extends Check implements PacketCheck {

    private static final double MAX_ROTATION_DEVIATION = 60.0;
    private static final double SUSPICIOUS_ZERO_THRESHOLD = 0.01;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        float lastYaw;
        float lastPitch;
        int zeroRotationCount;
        int totalPlaceCount;

        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (packet instanceof PlayerInteractBlockC2SPacket) {
            PlayerInteractBlockC2SPacket p = (PlayerInteractBlockC2SPacket) packet;
            PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

            state.totalPlaceCount++;

            BlockPos blockPos = p.getBlockHitResult().getBlockPos();
            Direction face = p.getBlockHitResult().getSide();

            double blockCenterX = blockPos.getX() + 0.5;
            double blockCenterY = blockPos.getY() + 0.5;
            double blockCenterZ = blockPos.getZ() + 0.5;

            double hitX = blockCenterX + face.getOffsetX() * 0.5;
            double hitY = blockCenterY + face.getOffsetY() * 0.5;
            double hitZ = blockCenterZ + face.getOffsetZ() * 0.5;

            double eyeX = player.getX();
            double eyeY = player.getY() + player.getEyeHeight();
            double eyeZ = player.getZ();

            double dx = hitX - eyeX;
            double dy = hitY - eyeY;
            double dz = hitZ - eyeZ;

            double expectedYaw = Math.toDegrees(Math.atan2(-dx, dz));
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            double expectedPitch = Math.toDegrees(Math.atan2(-dy, horizontalDist));

            float actualYaw = player.getYaw();
            float actualPitch = player.getPitch();

            boolean isZeroRotation = Math.abs(actualYaw) < SUSPICIOUS_ZERO_THRESHOLD
                    && Math.abs(actualPitch) < SUSPICIOUS_ZERO_THRESHOLD;
            if (isZeroRotation) {
                state.zeroRotationCount++;
            }

            if (state.zeroRotationCount > 5 && state.totalPlaceCount > 10) {
                double zeroRatio = (double) state.zeroRotationCount / state.totalPlaceCount;
                if (zeroRatio > 0.8) {
                    increaseBuffer(player, 1.0);
                    if (getBuffer(player) > 3.0) {
                        flag(player);
                        resetBuffer(player);
                    }
                    return;
                }
            }

            double yawDiff = Math.abs(normalizeAngle(actualYaw - expectedYaw));
            double pitchDiff = Math.abs(normalizeAngle(actualPitch - expectedPitch));
            double totalDeviation = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

            if (totalDeviation > MAX_ROTATION_DEVIATION) {
                increaseBuffer(player, (totalDeviation - MAX_ROTATION_DEVIATION) / MAX_ROTATION_DEVIATION);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }

    private static double normalizeAngle(double angle) {
        while (angle > 180.0) angle -= 360.0;
        while (angle < -180.0) angle += 360.0;
        return angle;
    }
}
