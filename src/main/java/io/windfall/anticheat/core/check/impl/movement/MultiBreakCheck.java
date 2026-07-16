package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Multi Break", stableKey = "windfall.movement.multibreak", decay = 0.02, setbackVl = 10)
public class MultiBreakCheck extends Check implements PacketCheck {

    private static final int MAX_BREAKS_PER_TICK = 1;
    private static final int BUFFER_THRESHOLD = 2;

    private static final class PlayerState {
        int breaksThisTick;
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
        if (!(packet instanceof PlayerActionC2SPacket)) return;

        PlayerActionC2SPacket action = (PlayerActionC2SPacket) packet;
        if (action.getAction() != PlayerActionC2SPacket.Action.START_DESTROY_BLOCK) return;

        PlayerState state = getState(player);
        long currentTick = player.getTickCount();

        if (currentTick != state.lastTick) {
            state.breaksThisTick = 0;
            state.lastTick = currentTick;
        }

        state.breaksThisTick++;

        if (state.breaksThisTick > MAX_BREAKS_PER_TICK) {
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
