package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Wrong Break", stableKey = "windfall.movement.wrongbreak", decay = 0.02, setbackVl = 10)
public class WrongBreakCheck extends Check implements PacketCheck {

    private static final double MAX_Y_DEVIATION = 2.0;
    private static final int BUFFER_THRESHOLD = 3;

    private static final class PlayerState {
        double lastBreakX, lastBreakY, lastBreakZ;
        boolean hasLastBreak;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        if (action.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return;

        PlayerState state = getState(player);
        BlockPos blockPos = action.getPos();

        int blockX = blockPos.getX();
        int blockY = blockPos.getY();
        int blockZ = blockPos.getZ();

        double yDeviation = Math.abs(player.getY() - blockY);

        if (yDeviation > MAX_Y_DEVIATION) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.5);
        }

        if (state.hasLastBreak) {
            double dx = blockX - state.lastBreakX;
            double dz = blockZ - state.lastBreakZ;
            double dist = Math.sqrt(dx * dx + dz * dz);

            if (dist > 10.0) {
                increaseBuffer(player, 2.0);
                if (getBuffer(player) > BUFFER_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        state.lastBreakX = blockX;
        state.lastBreakY = blockY;
        state.lastBreakZ = blockZ;
        state.hasLastBreak = true;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
