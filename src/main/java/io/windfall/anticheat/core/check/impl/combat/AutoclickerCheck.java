package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Autoclicker A", stableKey="windfall.combat.autoclicker", decay=0.02, setbackVl=30)
public class AutoclickerCheck extends Check implements PacketCheck {

    private static final int MIN_CLICKS_FOR_ANALYSIS = 10;
    private static final double MIN_STDDEV_THRESHOLD = 5.0;
    private static final int INTERVAL_HISTORY_SIZE = 50;

    private final ConcurrentHashMap<UUID, ClickState> clickStates = new ConcurrentHashMap<>();

    private static class ClickState {
        final List<Long> timestamps = new ArrayList<>();
        final Deque<Long> intervals = new ArrayDeque<>();
        long lastClickTime;
    }

    private ClickState getState(UUID uuid) {
        return clickStates.computeIfAbsent(uuid, k -> new ClickState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;

        final boolean[] isAttack = {false};
        p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
            public void interact(net.minecraft.util.Hand hand) {}
            public void attack() { isAttack[0] = true; }
            public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {}
        });
        if (!isAttack[0]) return;

        ClickState state = getState(player.getUuid());
        long now = System.currentTimeMillis();

        state.timestamps.add(now);
        state.timestamps.removeIf(t -> now - t > 1000);

        if (state.lastClickTime > 0) {
            long interval = now - state.lastClickTime;
            if (interval > 0 && interval < 1000) {
                state.intervals.addLast(interval);
                while (state.intervals.size() > INTERVAL_HISTORY_SIZE) state.intervals.pollFirst();
            }
        }
        state.lastClickTime = now;

        int cps = state.timestamps.size();
        if (cps > 20) {
            increaseBuffer(player, (cps - 20) / 10.0);
            if (getBuffer(player) > 3.0) { flag(player); resetBuffer(player); }
            return;
        }

        if (state.intervals.size() >= MIN_CLICKS_FOR_ANALYSIS) {
            double mean = 0;
            for (long iv : state.intervals) mean += iv;
            mean /= state.intervals.size();

            double variance = 0;
            for (long iv : state.intervals) variance += (iv - mean) * (iv - mean);
            variance /= state.intervals.size();
            double stdDev = Math.sqrt(variance);

            if (stdDev < MIN_STDDEV_THRESHOLD && mean < 80) {
                double consistency = 1.0 - (stdDev / Math.max(mean, 1.0));
                increaseBuffer(player, consistency * 1.2);
                if (getBuffer(player) > 3.0) { flag(player); resetBuffer(player); }
                return;
            }
        }

        decreaseBuffer(player, 0.5);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {}

    @Override
    public void removePlayer(UUID uuid) {
        clickStates.remove(uuid);
    }
}
