package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Creative A", stableKey="windfall.packet.creative", decay=0.02, setbackVl=20)
public class CreativeCheck extends Check implements PacketCheck {

    private static final int MAX_CREATIVE_ACTIONS_PER_TICK = 5;

    private final ConcurrentHashMap<java.util.UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerState {
        int creativeActionsThisTick = 0;
    }

    private PlayerState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof CreativeInventoryActionC2SPacket)) return;

        PlayerState state = getState(player);

        // Non-creative mode check
        try {
            net.minecraft.server.network.ServerPlayerEntity sp = player.getServerPlayer();
            if (sp != null && sp.interactionManager != null) {
                GameMode gameMode = sp.interactionManager.getGameMode();
                if (gameMode != GameMode.CREATIVE) {
                    increaseBuffer(player, 3.0);
                    flag(player);
                    resetBuffer(player);
                    kickPlayer(player, "Creative action in non-creative mode");
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        // Rate limit per tick
        state.creativeActionsThisTick++;
        if (state.creativeActionsThisTick > MAX_CREATIVE_ACTIONS_PER_TICK) {
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
                kickPlayer(player, "Too many creative actions per tick");
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public void onTick(WindfallPlayer player) {
        PlayerState state = getState(player);
        state.creativeActionsThisTick = 0;
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
