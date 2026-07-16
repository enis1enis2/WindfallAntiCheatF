package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="PositionBreak A", stableKey="windfall.movement.positionbreak", decay=0.02, setbackVl=20)
public class PositionBreakCheck extends Check implements PacketCheck {

    private static final double HARD_LIMIT = 5.0;
    private static final int TRACKING_WINDOW = 20;
    private static final double AVG_DISTANCE_THRESHOLD = 4.8;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        Deque<Double> recentDistances = new ArrayDeque<>();

        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        if (action.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return;

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

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());
        state.recentDistances.addLast(distance);
        while (state.recentDistances.size() > TRACKING_WINDOW) {
            state.recentDistances.pollFirst();
        }

        if (distance > HARD_LIMIT) {
            increaseBuffer(player, (distance - HARD_LIMIT) / HARD_LIMIT);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }

        if (state.recentDistances.size() >= TRACKING_WINDOW) {
            double avg = state.recentDistances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            if (avg > AVG_DISTANCE_THRESHOLD) {
                increaseBuffer(player, (avg - AVG_DISTANCE_THRESHOLD) / AVG_DISTANCE_THRESHOLD);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                }
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
}
