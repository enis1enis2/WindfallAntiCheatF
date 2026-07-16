package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Criticals A", stableKey="windfall.combat.criticals", decay=0.02, setbackVl=20)
public class CriticalsCheck extends Check implements PacketCheck {

    private static final double CRITICAL_MIN = 0.11;
    private static final double CRITICAL_MAX = 0.5;
    private static final int MAX_INVALID_CRITICALS = 4;

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private static class PlayerState {
        int invalidCriticalStreak;
        double lastAttackY;
    }

    private PlayerState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
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

        PlayerState state = getState(player.getUuid());

        if (player.isFlying() || player.isGliding() || player.isSwimming()) {
            state.invalidCriticalStreak = 0;
            decreaseBuffer(player, 0.1);
            return;
        }

        if (!player.isOnGround() && !player.isSneaking()) {
            double deltaY = Math.abs(player.getY() - player.getLastY());
            double deltaY2 = Math.abs(player.getY() - player.getLastLastY());

            boolean validVerticalMotion = (deltaY > CRITICAL_MIN && deltaY < CRITICAL_MAX)
                    || (deltaY2 > CRITICAL_MIN && deltaY2 < CRITICAL_MAX);

            boolean hasVelocityY = Math.abs(player.getVelocityY()) > 0.05;

            boolean wasOnGround = player.isLastOnGround();
            boolean jumpStart = wasOnGround && deltaY > 0.0;

            if (validVerticalMotion || hasVelocityY || jumpStart) {
                state.invalidCriticalStreak = 0;
                decreaseBuffer(player, 0.2);
            } else {
                state.invalidCriticalStreak++;
                if (state.invalidCriticalStreak >= MAX_INVALID_CRITICALS) {
                    flagWithSetback(player);
                    state.invalidCriticalStreak = 0;
                } else {
                    increaseBuffer(player, 0.5);
                    if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
                }
            }
        } else {
            state.invalidCriticalStreak = 0;
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {}

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }
}
