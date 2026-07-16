package io.windfall.anticheat.core.check.impl.inventory;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Inventory A", stableKey="windfall.inventory.inventory", decay=0.02, setbackVl=15,
    compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class InventoryCheck extends Check implements PacketCheck {

    private static final int MAX_CLICKS_PER_SECOND = 20;
    private static final long CLICK_WINDOW_MS = 50;

    private static final class PlayerState {
        long lastClickTime;
        int clickCount;
        int clicksThisSecond;
        long secondStart;
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

        if (packet instanceof ClickSlotC2SPacket) {
            handleClickWindow(player);
        } else if (packet instanceof CreativeInventoryActionC2SPacket) {
            handleCreativeSlot(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private void handleClickWindow(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();

        if (now - state.lastClickTime < CLICK_WINDOW_MS) {
            state.clickCount++;
            if (state.clickCount > 5) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            state.clickCount = 0;
        }
        state.lastClickTime = now;

        if (now - state.secondStart > 1000) {
            state.clicksThisSecond = 0;
            state.secondStart = now;
        }
        state.clicksThisSecond++;
        if (state.clicksThisSecond > MAX_CLICKS_PER_SECOND) {
            flag(player);
        }
    }

    private void handleCreativeSlot(WindfallPlayer player) {
        ServerPlayerEntity sp = player.getServerPlayer();
        if (sp == null) return;
        if (sp.getGameMode() != GameMode.CREATIVE) {
            flag(player);
        }
    }
}
