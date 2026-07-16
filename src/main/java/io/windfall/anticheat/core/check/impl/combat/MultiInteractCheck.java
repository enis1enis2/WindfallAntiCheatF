package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="MultiInteract A", stableKey="windfall.combat.multiinteract", decay=0.02, setbackVl=20)
public class MultiInteractCheck extends Check implements PacketCheck {

    private static final long TICK_WINDOW_MS = 55;
    private static final int MULTI_HIT_THRESHOLD = 2;

    private final ConcurrentHashMap<UUID, InteractState> stateMap = new ConcurrentHashMap<>();

    private static class InteractState {
        final Set<Integer> tickEntityIds = new HashSet<>();
        long tickWindowStart;
        long lastInteractTime;
    }

    private InteractState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new InteractState());
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
            public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {
                isAttack[0] = true;
                try {
                    net.minecraft.entity.Entity e = p.getEntity(player.getServerPlayer().getServerWorld());
                    if (e != null) targetEntityId[0] = e.getId();
                } catch (Exception ignored) {}
            }
        });
        if (!isAttack[0] || targetEntityId[0] == -1) return;

        InteractState state = getState(player.getUuid());
        long now = System.currentTimeMillis();

        if (now - state.tickWindowStart > TICK_WINDOW_MS) {
            state.tickEntityIds.clear();
            state.tickWindowStart = now;
        }

        state.tickEntityIds.add(targetEntityId[0]);

        if (state.tickEntityIds.size() >= MULTI_HIT_THRESHOLD) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
        }

        if (now - state.lastInteractTime < 5) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
        }

        state.lastInteractTime = now;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {}

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }
}
