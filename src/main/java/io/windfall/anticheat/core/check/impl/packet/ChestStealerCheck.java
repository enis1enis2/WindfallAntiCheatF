package io.windfall.anticheat.core.check.impl.packet;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Chest Stealer A", stableKey="windfall.packet.cheststealer", decay=0.01, setbackVl=15)
public class ChestStealerCheck extends Check implements PacketCheck {

    private static final int MAX_CLICKS_PER_WINDOW = 40;
    private static final long WINDOW_TIMEOUT_MS = 3000;
    private static final int FAST_CLICK_THRESHOLD = 6;
    private static final long FAST_CLICK_WINDOW_MS = 500;
    private static final int MAX_ITEMS_PER_SECOND = 15;

    private static final class PlayerState {
        int clicksThisWindow;
        long windowOpenTime;
        boolean windowOpen;
        final ArrayDeque<Long> clickTimestamps = new ArrayDeque<>();
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
        if (!enabled) return;

        PlayerState state = getState(player);

        if (packet instanceof ClickSlotC2SPacket) {
            handleClick(player, state);
        } else if (packet instanceof CloseHandledScreenC2SPacket) {
            handleClose(player, state);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
        if (packet instanceof OpenScreenS2CPacket) {
            PlayerState state = getState(player);
            state.windowOpen = true;
            state.windowOpenTime = System.currentTimeMillis();
            state.clicksThisWindow = 0;
        }
    }

    private void handleClick(WindfallPlayer player, PlayerState state) {
        if (!state.windowOpen) return;

        long now = System.currentTimeMillis();
        state.clicksThisWindow++;
        state.clickTimestamps.addLast(now);

        while (!state.clickTimestamps.isEmpty() && now - state.clickTimestamps.peekFirst() > FAST_CLICK_WINDOW_MS) {
            state.clickTimestamps.removeFirst();
        }

        if (state.clicksThisWindow > MAX_CLICKS_PER_WINDOW) {
            flag(player);
            return;
        }

        if (state.clickTimestamps.size() > FAST_CLICK_THRESHOLD) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.05);
        }
    }

    private void handleClose(WindfallPlayer player, PlayerState state) {
        if (!state.windowOpen) return;

        long now = System.currentTimeMillis();
        long windowDuration = now - state.windowOpenTime;
        if (windowDuration < WINDOW_TIMEOUT_MS && state.clicksThisWindow > MAX_ITEMS_PER_SECOND) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        state.windowOpen = false;
        state.clicksThisWindow = 0;
    }
}
