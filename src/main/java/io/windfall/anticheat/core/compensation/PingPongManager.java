package io.windfall.anticheat.core.compensation;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PingPongManager {
    private final WindfallMod mod;
    private final Map<UUID, PingState> pingStates = new ConcurrentHashMap<>();

    static class PingState {
        long preChangeNano;
        long postChangeNano;
        long preChangeTick;
        long postChangeTick;
    }

    public PingPongManager(WindfallMod mod) { this.mod = mod; }

    public void onTickStart(WindfallPlayer player) {
        PingState state = pingStates.computeIfAbsent(player.getUuid(), k -> new PingState());
        state.preChangeNano = System.nanoTime();
        state.preChangeTick = mod.getCheckManager().getTickCounter();
        mod.getTransactionManager().sendTransaction(player);
    }

    public void onTickEnd(WindfallPlayer player) {
        PingState state = pingStates.computeIfAbsent(player.getUuid(), k -> new PingState());
        state.postChangeNano = System.nanoTime();
        state.postChangeTick = mod.getCheckManager().getTickCounter();
    }

    public int getPingDelta(UUID uuid) {
        PingState state = pingStates.get(uuid);
        if (state == null) return 0;
        return (int) ((state.postChangeNano - state.preChangeNano) / 1_000_000);
    }

    public void onPlayerQuit(UUID uuid) {
        pingStates.remove(uuid);
    }
}
