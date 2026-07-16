package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Chat A", stableKey="windfall.packet.chat", decay=0.02, setbackVl=20)
public class ChatCheck extends Check implements PacketCheck {

    private static final int MAX_MESSAGES_PER_MINUTE = 60;
    private static final int MAX_BURST_MESSAGES = 4;
    private static final long BURST_WINDOW_MS = 2000;
    private static final long MINUTE_WINDOW_MS = 60000;

    private final ConcurrentHashMap<java.util.UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerState {
        Deque<Long> messageTimestamps = new ArrayDeque<>();
        int perMinuteViolationCount = 0;
        int burstViolationCount = 0;
    }

    private PlayerState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof ChatMessageC2SPacket)) return;

        PlayerState state = getState(player);
        long now = System.currentTimeMillis();

        // Add current message timestamp
        state.messageTimestamps.addLast(now);

        // Remove timestamps older than 1 minute
        while (!state.messageTimestamps.isEmpty() && now - state.messageTimestamps.peekFirst() > MINUTE_WINDOW_MS) {
            state.messageTimestamps.pollFirst();
        }

        // Per-minute rate limit check
        if (state.messageTimestamps.size() > MAX_MESSAGES_PER_MINUTE) {
            state.perMinuteViolationCount++;
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
                kickPlayer(player, "Chat rate limit exceeded (per-minute)");
            }
            return;
        }

        // Burst detection: count messages in last 2 seconds
        int burstCount = 0;
        for (Long ts : state.messageTimestamps) {
            if (now - ts <= BURST_WINDOW_MS) {
                burstCount++;
            }
        }

        if (burstCount > MAX_BURST_MESSAGES) {
            state.burstViolationCount++;
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
                kickPlayer(player, "Chat burst detected");
            }
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
