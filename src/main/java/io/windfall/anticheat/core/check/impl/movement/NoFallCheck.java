package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="NoFall A", stableKey="windfall.movement.nofall", decay=0.02, setbackVl=20)
public class NoFallCheck extends Check implements PacketCheck {

    private static final double FALL_THRESHOLD = 0.5;
    private static final double GROUND_VELOCITY_THRESHOLD = 0.001;
    private static final int AIRBORNE_TICKS_MIN = 4;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        double fallDistance;
        int airborneTicks;
        double lastY;
        boolean wasClaimedGround;
        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

        double deltaY = player.getY() - player.getLastY();
        boolean claimedGround = player.isOnGround();
        double verticalSpeed = player.getVerticalSpeed();

        if (!claimedGround) {
            state.airborneTicks++;
            if (deltaY < -0.01) {
                state.fallDistance += Math.abs(deltaY);
            }
        }

        if (state.airborneTicks >= AIRBORNE_TICKS_MIN
            && claimedGround
            && state.fallDistance > FALL_THRESHOLD
            && verticalSpeed < GROUND_VELOCITY_THRESHOLD) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
            state.fallDistance = 0;
            state.airborneTicks = 0;
        } else if (claimedGround) {
            if (state.fallDistance < 0.01) {
                decreaseBuffer(player, 0.2);
            }
            state.airborneTicks = 0;
        }

        if (claimedGround && !player.isLastOnGround()
            && state.lastY - player.getY() > 0.5
            && verticalSpeed < GROUND_VELOCITY_THRESHOLD) {
            increaseBuffer(player, 0.6);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        if (deltaY < -0.5 && claimedGround) {
            increaseBuffer(player, 0.4);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        state.lastY = player.getY();
        state.wasClaimedGround = claimedGround;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }
}
