package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="SwordBlock A", stableKey="windfall.combat.swordblock", decay=0.02, setbackVl=20, compat={CompatFlag.VERSION_LEGACY})
public class SwordBlockCheck extends Check implements PacketCheck {

    private static final long BLOCK_ATTACK_GAP_THRESHOLD_MS = 200;
    private static final int MIN_CONSECUTIVE_FLAGS = 4;
    private static final double BLOCK_ATTACK_RATIO_THRESHOLD = 0.7;
    private static final int MIN_ATTACKS_FOR_RATIO = 8;
    private static final long RATIO_WINDOW_MS = 500;

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private static class PlayerState {
        long lastBlockTimestamp;
        boolean waitingForAttack;
        int consecutiveGapFlags;
        final Deque<Long> attackTimestamps = new ArrayDeque<>();
        final Deque<Long> blockTimestamps = new ArrayDeque<>();
    }

    private PlayerState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket) {
            PlayerState state = getState(player.getUuid());
            long now = System.currentTimeMillis();
            state.lastBlockTimestamp = now;
            state.waitingForAttack = true;
            state.blockTimestamps.addLast(now);
            cleanupWindow(state.blockTimestamps, now, RATIO_WINDOW_MS);
        }

        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;

        final boolean[] isAttack = {false};
        p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
            public void interact(net.minecraft.util.Hand hand) {}
            public void attack() { isAttack[0] = true; }
            public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {}
        });
        if (!isAttack[0]) return;

        PlayerState state = getState(player.getUuid());
        long now = System.currentTimeMillis();
        state.attackTimestamps.addLast(now);
        cleanupWindow(state.attackTimestamps, now, RATIO_WINDOW_MS);

        if (state.waitingForAttack) {
            long gap = now - state.lastBlockTimestamp;
            state.waitingForAttack = false;

            if (gap < BLOCK_ATTACK_GAP_THRESHOLD_MS) {
                state.consecutiveGapFlags++;
                if (state.consecutiveGapFlags >= MIN_CONSECUTIVE_FLAGS) {
                    flag(player);
                    resetBuffer(player);
                    state.consecutiveGapFlags = 0;
                } else {
                    increaseBuffer(player, 0.5);
                    if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
                }
            } else {
                state.consecutiveGapFlags = 0;
                decreaseBuffer(player, 0.2);
            }
        }

        int attacksInWindow = state.attackTimestamps.size();
        int blocksInWindow = state.blockTimestamps.size();
        if (attacksInWindow >= MIN_ATTACKS_FOR_RATIO) {
            double ratio = (double) blocksInWindow / attacksInWindow;
            if (ratio > BLOCK_ATTACK_RATIO_THRESHOLD) {
                increaseBuffer(player, (ratio - BLOCK_ATTACK_RATIO_THRESHOLD) * 3.0);
                if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
            } else {
                decreaseBuffer(player, 0.1);
            }
        }
    }

    private void cleanupWindow(Deque<Long> deque, long now, long windowMs) {
        while (!deque.isEmpty() && now - deque.peekFirst() > windowMs) {
            deque.pollFirst();
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {}

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }
}
