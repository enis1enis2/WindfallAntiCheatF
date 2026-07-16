package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.check.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Criticals A", stableKey = "windfall.combat.criticals", decay = 0.01, setbackVl = 10)
public class CriticalsCheck extends Check implements PacketCheck {

    private static final double MIN_DELTA_Y_CRITICAL = 0.11;
    private static final double MAX_DELTA_Y_CRITICAL = 0.5;
    private static final int INVALID_THRESHOLD = 4;

    private static final class PlayerState {
        int attacksSinceGround;
        int consecutiveInvalid;
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
        if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p) {
            final boolean[] isAttack = {false};
            p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
                public void interact(net.minecraft.util.Hand hand) {}
                public void attack() { isAttack[0] = true; }
                public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {}
            });
            if (isAttack[0]) {
                handleAttack(player);
            }
        } else if (isMovementPacket(packet)) {
            handleMovement(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private void handleAttack(WindfallPlayer player) {
        PlayerState state = getState(player.getUuid());

        if (player.isOnGround()) {
            state.consecutiveInvalid = Math.max(0, state.consecutiveInvalid - 1);
            return;
        }

        double deltaY = player.getDeltaY();
        boolean validCritMotion = deltaY > MIN_DELTA_Y_CRITICAL && deltaY < MAX_DELTA_Y_CRITICAL;

        if (!validCritMotion && deltaY >= -0.01) {
            state.consecutiveInvalid++;
            if (state.consecutiveInvalid >= INVALID_THRESHOLD) {
                flagWithSetback(player);
                state.consecutiveInvalid = 0;
            }
        } else {
            state.consecutiveInvalid = Math.max(0, state.consecutiveInvalid - 1);
        }
    }

    private void handleMovement(WindfallPlayer player) {
        PlayerState state = getState(player.getUuid());
        if (player.isOnGround()) {
            state.attacksSinceGround = 0;
        } else {
            state.attacksSinceGround++;
        }
    }

    private boolean isMovementPacket(Object packet) {
        return packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
    }
}
