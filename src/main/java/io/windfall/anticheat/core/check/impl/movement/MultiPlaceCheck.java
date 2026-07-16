package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Multi Place", stableKey = "windfall.movement.multiplace", decay = 0.02, setbackVl = 10)
public class MultiPlaceCheck extends Check implements PacketCheck {

    private static final int MAX_PLACES_PER_TICK = 1;
    private static final int BUFFER_THRESHOLD = 2;

    private static final class PlayerState {
        int placesThisTick;
        long lastTick;
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
        if (!(packet instanceof PlayerInteractBlockC2SPacket)) return;

        PlayerInteractBlockC2SPacket p = (PlayerInteractBlockC2SPacket) packet;
        if (p.getBlockHitResult().getSide() == null) return;

        PlayerState state = getState(player);
        long currentTick = player.getTickCount();

        if (currentTick != state.lastTick) {
            state.placesThisTick = 0;
            state.lastTick = currentTick;
        }

        state.placesThisTick++;

        if (state.placesThisTick > MAX_PLACES_PER_TICK) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > BUFFER_THRESHOLD) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
