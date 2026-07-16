package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="FarBreak A", stableKey="windfall.movement.farbreak", decay=0.02, setbackVl=20)
public class FarBreakCheck extends Check implements PacketCheck {

    private static final double HARD_LIMIT = 5.3;
    private static final double SOFT_LIMIT = 5.0;
    private static final double HARD_INCREMENT = 1.0;
    private static final double SOFT_INCREMENT = 0.3;

    private final Map<UUID, Long> lastFlagTime = new ConcurrentHashMap<>();

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        PlayerActionC2SPacket.Action actionType = action.getAction();

        if (actionType != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK
                && actionType != PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
            return;
        }

        BlockPos blockPos = action.getPos();
        double blockCenterX = blockPos.getX() + 0.5;
        double blockCenterY = blockPos.getY() + 0.5;
        double blockCenterZ = blockPos.getZ() + 0.5;

        double eyeX = player.getX();
        double eyeY = player.getY() + player.getEyeHeight();
        double eyeZ = player.getZ();

        double dx = eyeX - blockCenterX;
        double dy = eyeY - blockCenterY;
        double dz = eyeZ - blockCenterZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance > HARD_LIMIT) {
            increaseBuffer(player, HARD_INCREMENT);
            if (getBuffer(player) > 3.0) {
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
            if (getBuffer(player) > 5.0) {
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
    public void removePlayer(UUID uuid) {
        lastFlagTime.remove(uuid);
    }
}
