package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.check.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Multi Interact A", stableKey = "windfall.combat.multiinteract", decay = 0.01, setbackVl = 15)
public class MultiInteractCheck extends Check implements PacketCheck {

    private static final int MAX_ENTITIES_PER_TICK = 2;
    private static final long TICK_WINDOW_MS = 60;

    private static final class PlayerState {
        final Set<Integer> entitiesThisTick = new HashSet<>();
        long lastAttackTime;
        int consecutiveViolations;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;

        final boolean[] isAttack = {false};
        final int[] targetEntityId = {-1};
        p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
            public void interact(net.minecraft.util.Hand hand) {}
            public void attack() {
                isAttack[0] = true;
                try {
                    net.minecraft.entity.Entity e = p.getEntity(player.getServerPlayer().getServerWorld());
                    if (e != null) targetEntityId[0] = e.getId();
                } catch (Exception ignored) {}
            }
            public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {}
        });
        if (!isAttack[0]) return;

        PlayerState state = getState(player.getUuid());
        long now = System.currentTimeMillis();

        if (now - state.lastAttackTime > TICK_WINDOW_MS) {
            state.entitiesThisTick.clear();
        }
        state.lastAttackTime = now;

        int targetId;
        try {
            net.minecraft.entity.Entity e = p.getEntity(player.getServerPlayer().getServerWorld());
            targetId = e != null ? e.getId() : -1;
        } catch (Exception e) {
            targetId = -1;
        }

        if (targetId == -1) return;

        state.entitiesThisTick.add(targetId);

        if (state.entitiesThisTick.size() > MAX_ENTITIES_PER_TICK) {
            state.consecutiveViolations++;
            if (state.consecutiveViolations >= 3) {
                flag(player);
                state.consecutiveViolations = 0;
            }
            resetBuffer(player);
        } else {
            state.consecutiveViolations = Math.max(0, state.consecutiveViolations - 1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
