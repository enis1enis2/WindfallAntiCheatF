package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="ChestStealer A", stableKey="windfall.packet.cheststealer", decay=0.02, setbackVl=20)
public class ChestStealerCheck extends Check implements PacketCheck {

    private static final int MAX_CLICKS_PER_WINDOW = 40;
    private static final int FAST_CLICK_THRESHOLD = 6;
    private static final long FAST_CLICK_WINDOW_MS = 500;
    private static final long SHORT_SESSION_THRESHOLD_MS = 3000;
    private static final int SHORT_SESSION_MIN_CLICKS = 15;

    private final ConcurrentHashMap<java.util.UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static class PlayerState {
        int clicksInSession = 0;
        long windowOpenTime = 0;
        long lastClickTime = 0;
        int recentClickCount = 0;
        long recentClickWindowStart = 0;
        boolean windowOpen = false;
    }

    private PlayerState getState(WindfallPlayer player) {
        return playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;

        PlayerState state = getState(player);

        if (packet instanceof ClickSlotC2SPacket) {
            if (!state.windowOpen) return;

            long now = System.currentTimeMillis();
            state.clicksInSession++;
            state.lastClickTime = now;

            // Per-window click limit
            if (state.clicksInSession > MAX_CLICKS_PER_WINDOW) {
                increaseBuffer(player, 2.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                    kickPlayer(player, "Too many clicks in window session");
                }
                return;
            }

            // Fast-click burst detection
            if (now - state.recentClickWindowStart > FAST_CLICK_WINDOW_MS) {
                state.recentClickCount = 0;
                state.recentClickWindowStart = now;
            }
            state.recentClickCount++;

            if (state.recentClickCount > FAST_CLICK_THRESHOLD) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }

            // Short session detection
            long sessionDuration = now - state.windowOpenTime;
            if (sessionDuration < SHORT_SESSION_THRESHOLD_MS && state.clicksInSession >= SHORT_SESSION_MIN_CLICKS) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else if (packet instanceof CloseHandledScreenC2SPacket) {
            // Window closed
            if (state.windowOpen) {
                state.windowOpen = false;
                state.clicksInSession = 0;
                state.recentClickCount = 0;
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
        // Detect window open from server
        if (packet instanceof net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket) {
            PlayerState state = getState(player);
            state.windowOpen = true;
            state.windowOpenTime = System.currentTimeMillis();
            state.clicksInSession = 0;
            state.recentClickCount = 0;
        }
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
