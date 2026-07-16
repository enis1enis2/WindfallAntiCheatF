package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="FastHeal A", stableKey="windfall.combat.fastheal", decay=0.02, setbackVl=20)
public class FastHealCheck extends Check implements PacketCheck {

    private static final long HEAL_THRESHOLD_MS = 500;
    private static final int FAST_HEAL_STREAK_THRESHOLD = 3;

    private final ConcurrentHashMap<UUID, HealState> stateMap = new ConcurrentHashMap<>();

    private static class HealState {
        long lastHealTime;
        float lastHealth;
        int fastHealStreak;
    }

    private HealState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new HealState());
    }

    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {}

    @Override public void onPacketSend(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket p)) return;

        long now = System.currentTimeMillis();
        HealState state = getState(player.getUuid());

        float currentHealth = p.getHealth();
        float healthDelta = currentHealth - state.lastHealth;

        state.lastHealth = currentHealth;

        if (state.lastHealTime > 0 && (now - state.lastHealTime) < HEAL_THRESHOLD_MS && healthDelta > 0) {
            state.fastHealStreak++;
            double severity = 1.0 + (state.fastHealStreak * 0.5);
            increaseBuffer(player, severity);
            if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
        } else {
            state.fastHealStreak = Math.max(0, state.fastHealStreak - 1);
            decreaseBuffer(player, 0.3);
        }

        state.lastHealTime = now;
    }

    @Override public void removePlayer(UUID uuid) { stateMap.remove(uuid); }
}
