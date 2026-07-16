package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Timer A", stableKey="windfall.movement.timer", decay=0.01, setbackVl=20)
public class TimerCheck extends Check implements PacketCheck {

    private static final int MAX_PACKETS_PER_TICK = 22;
    private static final int ROLLING_WINDOW_SIZE = 20;
    private static final double AVG_THRESHOLD = 22.0;

    private final ConcurrentHashMap<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        int packetCount;
        final Deque<Integer> tickHistory = new ArrayDeque<>();
        double rollingAvg;
    }

    private PlayerState getState(UUID uuid) {
        return playerStates.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;

        PlayerState state = getState(player.getUuid());
        state.packetCount++;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public void onTick(WindfallPlayer player) {
        PlayerState state = getState(player.getUuid());

        int count = state.packetCount;
        state.packetCount = 0;

        state.tickHistory.addLast(count);
        while (state.tickHistory.size() > ROLLING_WINDOW_SIZE) state.tickHistory.pollFirst();

        double sum = 0;
        for (int c : state.tickHistory) sum += c;
        state.rollingAvg = state.tickHistory.isEmpty() ? 0 : sum / state.tickHistory.size();

        if (count > MAX_PACKETS_PER_TICK) {
            double excess = count - MAX_PACKETS_PER_TICK;
            increaseBuffer(player, excess / 5.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (state.rollingAvg > AVG_THRESHOLD) {
            increaseBuffer(player, (state.rollingAvg - AVG_THRESHOLD) / 10.0);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.2);
        }
    }

    @Override
    public void removePlayer(UUID uuid) {
        playerStates.remove(uuid);
    }
}
