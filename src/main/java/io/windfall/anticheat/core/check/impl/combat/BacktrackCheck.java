package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Backtrack A", stableKey="windfall.combat.backtrack", decay=0.01, setbackVl=15, compat={CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier=1.2)
public class BacktrackCheck extends Check implements PacketCheck {

    private static final long MAX_BACKTRACK_DELAY_MS = 500;
    private static final int MAX_DELAY_SAMPLES = 30;
    private static final double FLAG_BUFFER_THRESHOLD = 3.0;

    private static final class PlayerState {
        final ArrayDeque<Long> attackTimestamps = new ArrayDeque<>();
        long lastMovementTimestamp;
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
        PlayerState state = getState(player);

        if (packet instanceof PlayerMoveC2SPacket) {
            state.lastMovementTimestamp = System.currentTimeMillis();
            return;
        }

        if (packet instanceof PlayerInteractEntityC2SPacket p) {
            final boolean[] isAttack = {false};
            p.handle(new PlayerInteractEntityC2SPacket.Handler() {
                public void interact(net.minecraft.util.Hand hand) {}
                public void attack() { isAttack[0] = true; }
                public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {}
            });
            if (!isAttack[0]) return;

            long now = System.currentTimeMillis();
            long delay = now - state.lastMovementTimestamp;
            state.attackTimestamps.addLast(delay);

            while (state.attackTimestamps.size() > MAX_DELAY_SAMPLES) {
                state.attackTimestamps.removeFirst();
            }

            if (delay > MAX_BACKTRACK_DELAY_MS) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > FLAG_BUFFER_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {}
}
