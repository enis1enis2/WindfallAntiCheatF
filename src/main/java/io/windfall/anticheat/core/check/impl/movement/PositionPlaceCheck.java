package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="PositionPlace A", stableKey="windfall.movement.positionplace", decay=0.02, setbackVl=20)
public class PositionPlaceCheck extends Check implements PacketCheck {

    private static final double HARD_LIMIT = 5.3;
    private static final double SOFT_LIMIT = 5.0;
    private static final double HARD_INCREMENT = 1.5;
    private static final double SOFT_INCREMENT = 0.5;

    private final ConcurrentHashMap<UUID, Long> lastFlagTime = new ConcurrentHashMap<>();

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerInteractBlockC2SPacket)) return;

        PlayerInteractBlockC2SPacket p = (PlayerInteractBlockC2SPacket) packet;
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

        double dx = eyeX - hitX;
        double dy = eyeY - hitY;
        double dz = eyeZ - hitZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > HARD_LIMIT) {
            increaseBuffer(player, HARD_INCREMENT);
            if (getBuffer(player) > 2.0) {
                long now = System.currentTimeMillis();
                Long last = lastFlagTime.get(player.getUuid());
                if (last == null || now - last > 2000) {
                    flag(player);
                    resetBuffer(player);
                    lastFlagTime.put(player.getUuid(), now);
                }
            }
        } else if (distance > SOFT_LIMIT) {
            increaseBuffer(player, SOFT_INCREMENT);
            if (getBuffer(player) > 4.0) {
                long now = System.currentTimeMillis();
                Long last = lastFlagTime.get(player.getUuid());
                if (last == null || now - last > 3000) {
                    flag(player);
                    resetBuffer(player);
                    lastFlagTime.put(player.getUuid(), now);
                }
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
        lastFlagTime.remove(uuid);
    }
}
