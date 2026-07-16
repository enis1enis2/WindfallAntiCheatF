package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Elytra A", stableKey="windfall.movement.elytra", decay=0.02, setbackVl=20)
public class ElytraCheck extends Check implements PacketCheck {

    private static final double MAX_ELITE_HORIZ_SPEED = 1.5;
    private static final int HOVER_TICK_THRESHOLD = 40;
    private static final double HOVER_DELTA_THRESHOLD = 0.005;
    private static final double KICK_BOOST_THRESHOLD = 0.5;
    private static final int ASCENT_ELITE_MIN_TICKS = 5;

    private final Map<UUID, PlayerState> playerStates = new ConcurrentHashMap<>();

    private static final class PlayerState {
        int glidingTicks;
        double lastVerticalDelta;
        boolean wasGliding;
        boolean wasOnGround;
        int hoverCount;
        boolean flaggedHover;
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!enabled) return;
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.getProtocolVersion() < 47) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid(), k -> new PlayerState());
        boolean gliding = player.isGliding();
        boolean onGround = player.isOnGround();

        if (gliding) {
            if (!state.wasGliding) {
                state.glidingTicks = 0;
                state.hoverCount = 0;
                state.flaggedHover = false;
            }
            state.glidingTicks++;

            double horizSpeed = player.getHorizontalSpeed();
            double deltaY = player.getDeltaY();

            if (horizSpeed > MAX_ELITE_HORIZ_SPEED) {
                increaseBuffer(player, 0.5);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.05);
            }

            if (Math.abs(deltaY) < HOVER_DELTA_THRESHOLD && Math.abs(state.lastVerticalDelta) < HOVER_DELTA_THRESHOLD) {
                state.hoverCount++;
            } else {
                state.hoverCount = 0;
            }

            if (state.hoverCount >= HOVER_TICK_THRESHOLD && !state.flaggedHover) {
                increaseBuffer(player, 1.0);
                flag(player);
                state.flaggedHover = true;
            }

            if (state.glidingTicks >= ASCENT_ELITE_MIN_TICKS && deltaY > 0.15) {
                increaseBuffer(player, 0.4);
                if (getBuffer(player) > 2.5) {
                    flag(player);
                    resetBuffer(player);
                }
            }

            state.lastVerticalDelta = deltaY;
        } else {
            if (state.wasGliding && onGround && !state.wasOnGround) {
                double verticalBoost = player.getDeltaY();
                if (verticalBoost > KICK_BOOST_THRESHOLD) {
                    increaseBuffer(player, 1.0);
                    if (getBuffer(player) > 2.0) {
                        flag(player);
                        resetBuffer(player);
                    }
                }
            }
            state.glidingTicks = 0;
            state.hoverCount = 0;
            state.lastVerticalDelta = 0;
            state.flaggedHover = false;
        }

        state.wasGliding = gliding;
        state.wasOnGround = onGround;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(UUID uuid) {
        playerStates.remove(uuid);
    }
}
