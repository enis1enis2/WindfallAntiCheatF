package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Rotation Break A", stableKey = "windfall.movement.rotationbreak", decay = 0.02, setbackVl = 15)
public class RotationBreakCheck extends Check implements PacketCheck {

    private static final float MAX_ROTATION_DELTA = 45.0f;

    private static final class PlayerState {
        float breakStartYaw;
        float breakStartPitch;
        boolean breaking;
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
        PlayerActionC2SPacket.Action actionType = action.getAction();
        PlayerState state = getState(player);

        if (actionType == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) {
            state.breaking = true;
            state.breakStartYaw = player.getYaw();
            state.breakStartPitch = player.getPitch();
        } else if (actionType == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
            if (!state.breaking) return;
            state.breaking = false;

            float deltaYaw = Math.abs(player.getYaw() - state.breakStartYaw);
            float deltaPitch = Math.abs(player.getPitch() - state.breakStartPitch);

            if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;

            if (deltaYaw > MAX_ROTATION_DELTA || deltaPitch > MAX_ROTATION_DELTA) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
        } else if (actionType == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK) {
            state.breaking = false;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
