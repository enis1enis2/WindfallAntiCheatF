package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Chat A", stableKey="windfall.packet.chat", decay=0.01, setbackVl=15,
    compat={CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier=1.2)
public class ChatCheck extends Check implements PacketCheck {

    private static final int MAX_CHAT_PER_MINUTE = 60;
    private static final int MAX_CHAT_BURST = 4;
    private static final long CHAT_BURST_WINDOW_MS = 2000;

    private static final class PlayerState {
        final ArrayDeque<Long> chatTimestamps = new ArrayDeque<>();
        final ArrayDeque<Long> chatBurstWindow = new ArrayDeque<>();
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof ChatMessageC2SPacket)) return;

        PlayerState state = getState(player);
        long now = System.currentTimeMillis();
        state.chatTimestamps.addLast(now);
        state.chatBurstWindow.addLast(now);

        while (!state.chatTimestamps.isEmpty() && now - state.chatTimestamps.peekFirst() > 60000) {
            state.chatTimestamps.removeFirst();
        }
        while (!state.chatBurstWindow.isEmpty() && now - state.chatBurstWindow.peekFirst() > CHAT_BURST_WINDOW_MS) {
            state.chatBurstWindow.removeFirst();
        }

        int chatsPerMinute = state.chatTimestamps.size();
        int chatsInBurst = state.chatBurstWindow.size();

        if (chatsPerMinute > MAX_CHAT_PER_MINUTE) {
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
                kickPlayer(player, "Chat rate limit exceeded (per-minute)");
            }
        } else if (chatsInBurst > MAX_CHAT_BURST) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
                kickPlayer(player, "Chat burst detected");
            }
        } else {
            decreaseBuffer(player, 0.2);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    private void kickPlayer(WindfallPlayer player, String reason) {
        net.minecraft.server.network.ServerPlayerEntity sp = player.getServerPlayer();
        if (sp != null && sp.networkHandler != null) {
            sp.networkHandler.disconnect(Text.literal("[Windfall] " + reason));
        }
    }
}
