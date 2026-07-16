package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Rotation Place", stableKey = "windfall.movement.rotationplace", decay = 0.02, setbackVl = 10)
public class RotationPlaceCheck extends Check implements PacketCheck {

    private static final float MAX_YAW_DEVIATION = 45.0f;
    private static final float MAX_PITCH_DEVIATION = 45.0f;
    private static final int BUFFER_THRESHOLD = 5;

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerInteractBlockC2SPacket)) return;

        PlayerInteractBlockC2SPacket p = (PlayerInteractBlockC2SPacket) packet;
        if (p.getBlockHitResult().getSide() == null) return;

        BlockPos blockPos = p.getBlockHitResult().getBlockPos();
        double centerX = blockPos.getX() + 0.5;
        double centerY = blockPos.getY() + 0.5;
        double centerZ = blockPos.getZ() + 0.5;

        double eyeX = player.getX();
        double eyeY = player.getY() + player.getHeight();
        double eyeZ = player.getZ();

        double dx = centerX - eyeX;
        double dy = centerY - eyeY;
        double dz = centerZ - eyeZ;

        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float expectedYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float expectedPitch = (float) Math.toDegrees(Math.atan2(-dy, distXZ));

        float deltaYaw = Math.abs(player.getYaw() - expectedYaw);
        if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;

        float deltaPitch = Math.abs(player.getPitch() - expectedPitch);

        if (deltaYaw > MAX_YAW_DEVIATION || deltaPitch > MAX_PITCH_DEVIATION) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.5);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
