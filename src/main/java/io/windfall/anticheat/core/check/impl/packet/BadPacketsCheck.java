package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="BadPackets A", stableKey="windfall.packet.bad", decay=0.02, setbackVl=20)
public class BadPacketsCheck extends Check implements PacketCheck {

    private static final double MAX_Y = 400.0;
    private static final double MIN_Y = -64.0;
    private static final float MIN_YAW = -180.0f;
    private static final float MAX_YAW = 180.0f;
    private static final float MIN_PITCH = -90.0f;
    private static final float MAX_PITCH = 90.0f;
    private static final int MAX_CONSECUTIVE_DUPLICATES = 10;
    private static final int AUTO_CLICK_THRESHOLD = 20;

    private final ConcurrentHashMap<java.util.UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerState {
        long lastAttackTick = -1;
        int attackCountThisTick = 0;
        Deque<Long> attackTimestamps = new ArrayDeque<>();
        int consecutiveDuplicateCount = 0;
        Object lastPacket = null;
        boolean loggedIn = false;
    }

    private PlayerState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;

        PlayerState state = getState(player);
        state.loggedIn = true;

        // Movement check
        if (packet instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket move = (PlayerMoveC2SPacket) packet;
            double x = move.getX(0.0);
            double y = move.getY(0.0);
            double z = move.getZ(0.0);

            // NaN / Infinite coordinate check
            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)
                    || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
                increaseBuffer(player, 2.0);
                flag(player);
                resetBuffer(player);
                kickPlayer(player, "Invalid coordinates (NaN/Infinite)");
                return;
            }

            // Y out of bounds
            if (y > MAX_Y || y < MIN_Y) {
                increaseBuffer(player, 1.0);
                flag(player);
                resetBuffer(player);
                kickPlayer(player, "Y coordinate out of bounds");
                return;
            }

            // NaN / Infinite rotation check
            float yaw = move.getYaw(0.0f);
            float pitch = move.getPitch(0.0f);

            if (Float.isNaN(yaw) || Float.isNaN(pitch)
                    || Float.isInfinite(yaw) || Float.isInfinite(pitch)) {
                increaseBuffer(player, 2.0);
                flag(player);
                resetBuffer(player);
                kickPlayer(player, "Invalid rotation (NaN/Infinite)");
                return;
            }

            // Rotation range check
            if (yaw < MIN_YAW || yaw > MAX_YAW || pitch < MIN_PITCH || pitch > MAX_PITCH) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 2.0) {
                    flag(player);
                    resetBuffer(player);
                    kickPlayer(player, "Rotation out of range");
                    return;
                }
            }
        }

        // Duplicate packet detection
        if (packet.getClass().equals(state.lastPacket != null ? state.lastPacket.getClass() : null)) {
            state.consecutiveDuplicateCount++;
            if (state.consecutiveDuplicateCount >= MAX_CONSECUTIVE_DUPLICATES) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
                state.consecutiveDuplicateCount = 0;
            }
        } else {
            state.consecutiveDuplicateCount = 0;
        }
        state.lastPacket = packet;

        // Auto-clicker detection (attack packets)
        if (packet instanceof net.minecraft.network.packet.c2s.play.HandSwingC2SPacket) {
            long now = System.currentTimeMillis();
            state.attackTimestamps.addLast(now);
            // Purge entries older than 50ms window
            while (!state.attackTimestamps.isEmpty() && now - state.attackTimestamps.peekFirst() > 50) {
                state.attackTimestamps.pollFirst();
            }
            if (state.attackTimestamps.size() > AUTO_CLICK_THRESHOLD) {
                increaseBuffer(player, 2.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                    kickPlayer(player, "Auto-clicker detected");
                }
                state.attackTimestamps.clear();
            }
        }

        // Movement before login
        if (packet instanceof PlayerMoveC2SPacket && !state.loggedIn) {
            increaseBuffer(player, 2.0);
            flag(player);
            resetBuffer(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid);
    }

    private void kickPlayer(WindfallPlayer player, String reason) {
        net.minecraft.server.network.ServerPlayerEntity sp = player.getServerPlayer();
        if (sp != null && sp.networkHandler != null) {
            sp.networkHandler.disconnect(Text.literal("[Windfall] " + reason));
        }
    }
}
