package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Crash A", stableKey="windfall.packet.crash", decay=0.02, setbackVl=20)
public class CrashCheck extends Check implements PacketCheck {

    private static final int MAX_CHAT_LENGTH = 32767;
    private static final int CHAT_VIOLATION_THRESHOLD = 3;
    private static final double CREATIVE_BUFFER_THRESHOLD = 10.0;

    private final ConcurrentHashMap<java.util.UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerState {
        int oversizedChatCount = 0;
        double creativeBuffer = 0.0;
    }

    private PlayerState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;

        PlayerState state = getState(player);

        // Oversized chat message detection
        if (packet instanceof ChatMessageC2SPacket) {
            ChatMessageC2SPacket chatPacket = (ChatMessageC2SPacket) packet;
            String message = chatPacket.chatMessage();
            if (message != null && message.length() > MAX_CHAT_LENGTH) {
                state.oversizedChatCount++;
                increaseBuffer(player, 1.0);
                if (state.oversizedChatCount >= CHAT_VIOLATION_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                    kickPlayer(player, "Oversized chat packets");
                }
                return;
            }
        }

        // Suspicious creative packets
        if (packet instanceof CreativeInventoryActionC2SPacket) {
            state.creativeBuffer += 0.5;
            if (state.creativeBuffer > CREATIVE_BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
                state.creativeBuffer = 0.0;
                kickPlayer(player, "Suspicious creative packets");
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
