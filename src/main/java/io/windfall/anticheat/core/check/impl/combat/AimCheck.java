package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.check.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.util.MathUtil;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Aim A", stableKey = "windfall.combat.aim", decay = 0.01, setbackVl = 15, relaxMultiplier = 0.7)
public class AimCheck extends Check implements PacketCheck {

    private static final double INSTANT_SNAP_THRESHOLD = 180.0;
    private static final float ROTATION_MODULO = 360.0f;
    private static final int MIN_ROTATION_SAMPLES = 5;
    private static final double AIMBOT_YAW_VARIANCE_THRESHOLD = 0.5;
    private static final double MIN_DELTA_THRESHOLD = 0.015;
    private static final double SNAP_BUFFER_FLAG_THRESHOLD = 3.0;

    private static final class PlayerState {
        float lastYaw;
        float lastPitch;
        boolean hasRotation;
        double yawAccumulator;
        double pitchAccumulator;
        int rotationCount;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket movePacket)) return;

        PlayerState state = getState(player.getUuid());

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        if (!state.hasRotation) {
            state.lastYaw = yaw;
            state.lastPitch = pitch;
            state.hasRotation = true;
            return;
        }

        float deltaYaw = yaw - state.lastYaw;
        if (deltaYaw > 180) deltaYaw -= ROTATION_MODULO;
        if (deltaYaw < -180) deltaYaw += ROTATION_MODULO;

        float deltaPitch = pitch - state.lastPitch;

        double absDeltaYaw = Math.abs(deltaYaw);
        double absDeltaPitch = Math.abs(deltaPitch);

        if (absDeltaYaw < MIN_DELTA_THRESHOLD && absDeltaPitch < MIN_DELTA_THRESHOLD) {
            state.lastYaw = yaw;
            state.lastPitch = pitch;
            return;
        }

        if (player.getX() == player.getLastX()
                && player.getY() == player.getLastY()
                && player.getZ() == player.getLastZ()) {
            state.lastYaw = yaw;
            state.lastPitch = pitch;
            return;
        }

        if (absDeltaYaw > INSTANT_SNAP_THRESHOLD || absDeltaPitch > 90.0) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > SNAP_BUFFER_FLAG_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        }

        state.yawAccumulator += absDeltaYaw;
        state.pitchAccumulator += absDeltaPitch;
        state.rotationCount++;

        if (state.rotationCount >= MIN_ROTATION_SAMPLES) {
            double avgYawDelta = state.yawAccumulator / state.rotationCount;
            double avgPitchDelta = state.pitchAccumulator / state.rotationCount;

            if (avgYawDelta > 0.1 && avgPitchDelta < AIMBOT_YAW_VARIANCE_THRESHOLD) {
                increaseBuffer(player, 0.3);
                if (getBuffer(player) > 5.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }

            state.yawAccumulator = 0;
            state.pitchAccumulator = 0;
            state.rotationCount = 0;
        }

        state.lastYaw = yaw;
        state.lastPitch = pitch;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
