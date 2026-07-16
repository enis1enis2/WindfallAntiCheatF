package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="MultiPlace A", stableKey="windfall.movement.multiplace", decay=0.02, setbackVl=20)
public class MultiPlaceCheck extends Check implements PacketCheck {

    private static final int MAX_PLACES_PER_TICK = 4;
    private static final long TICK_WINDOW_MS = 50;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        int placesThisTick;
        long currentTickStart;
        int totalFlags;

        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerInteractBlockC2SPacket)) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());
        long now = System.currentTimeMillis();

        if (now - state.currentTickStart > TICK_WINDOW_MS) {
            state.placesThisTick = 0;
            state.currentTickStart = now;
        }

        state.placesThisTick++;

        if (state.placesThisTick > MAX_PLACES_PER_TICK) {
            increaseBuffer(player, (state.placesThisTick - MAX_PLACES_PER_TICK) * 1.5);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
                state.totalFlags++;
            }
        }

        if (state.placesThisTick > MAX_PLACES_PER_TICK + 2) {
            increaseBuffer(player, 2.0);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
                state.totalFlags++;
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }
}
