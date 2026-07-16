package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="RotationBreak A", stableKey="windfall.movement.rotationbreak", decay=0.02, setbackVl=20)
public class RotationBreakCheck extends Check implements PacketCheck {

    private static final double MAX_ROTATION_DEVIATION = 45.0;
    private static final double MAX_REACH = 6.0;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        float lastYaw;
        float lastPitch;
        BlockPos breakTarget;
        boolean trackingBreak;

        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (packet instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket p = (PlayerMoveC2SPacket) packet;
            float yaw = p.getYaw(0.0f);
            float pitch = p.getPitch(0.0f);
            PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());
            state.lastYaw = yaw;
            state.lastPitch = pitch;
            return;
        }

        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        if (action.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());
        BlockPos blockPos = action.getPos();
        state.breakTarget = blockPos;
        state.trackingBreak = true;

        double blockCenterX = blockPos.getX() + 0.5;
        double blockCenterY = blockPos.getY() + 0.5;
        double blockCenterZ = blockPos.getZ() + 0.5;

        double eyeX = player.getX();
        double eyeY = player.getY() + player.getEyeHeight();
        double eyeZ = player.getZ();

        double dx = blockCenterX - eyeX;
        double dy = blockCenterY - eyeY;
        double dz = blockCenterZ - eyeZ;

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > MAX_REACH) return;

        double expectedYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double expectedPitch = Math.toDegrees(Math.atan2(-dy, horizontalDist));

        float actualYaw = player.getYaw();
        float actualPitch = player.getPitch();

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
