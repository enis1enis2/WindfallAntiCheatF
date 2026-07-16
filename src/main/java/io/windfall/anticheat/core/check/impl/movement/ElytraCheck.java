package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects illegal elytra flight behaviour such as excessive horizontal speed,
 * sustained hovering (zero vertical movement), impossible upward boosts, and
 * ascent without a valid ground kick-off.
 *
 * <p><b>Variants checked:</b></p>
 * <ul>
 *   <li><b>Horizontal speed:</b> Flagged when horizontal speed exceeds
 *       {@link #ELYTRA_MAX_HORIZONTAL_SPEED} blocks/tick.</li>
 *   <li><b>Hover:</b> When vertical delta stays below {@link #ELYTRA_HOVER_DELTA}
 *       for more than {@link #ELYTRA_HOVER_TICK_THRESHOLD} ticks.</li>
 *   <li><b>Kick-boost:</b> A vertical boost greater than {@link #ELYTRA_KICKBOOST_MAX}
 *       while on the ground indicates a spoofed launch.</li>
 *   <li><b>Ascent:</b> Upward movement after 5+ elytra ticks that exceeds the
 *       expected minimum descent + tolerance is flagged as unnatural ascent.</li>
 * </ul>
 *
 * <p>This check is only active on protocol version 107+ (1.9+) where elytra exist.</p>
 *
 * @see VersionPhysics#hasElytra(int)
 * @see CompatFlag#RELAX_ON_MISMATCH
 */
@CheckData(name = "Elytra A", stableKey = "windfall.movement.elytra", decay = 0.01, setbackVl = 20, minVersion = 107, maxVersion = 999, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class ElytraCheck extends Check implements PacketCheck {

    private static final double ELYTRA_MAX_HORIZONTAL_SPEED = 1.5;
    private static final double ELYTRA_VERTICAL_TOLERANCE = 0.1;
    private static final double ELYTRA_MIN_DESCENT = -0.5;
    private static final int ELYTRA_HOVER_TICK_THRESHOLD = 40;
    private static final double ELYTRA_HOVER_DELTA = 0.005;
    private static final double ELYTRA_KICKBOOST_MAX = 0.5;

    private static final class PlayerState {
        int elytraHoverTicks;
        boolean wasGliding;
        double lastElytraDeltaY;
        int elytraTicks;
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
        if (!(packet instanceof PlayerMoveC2SPacket)) return;

        PlayerState state = getState(player);
        boolean isGliding = player.isGliding();
        int protocol = player.getProtocolVersion();

        if (!VersionPhysics.hasElytra(protocol)) return;

        if (isGliding) {
            state.elytraTicks++;
            handleElytraMovement(player, state);
            state.wasGliding = true;
        } else {
            if (state.wasGliding && state.elytraTicks > 0) {
                handleElytraLanding(player, state);
            }
            state.elytraTicks = 0;
            state.elytraHoverTicks = 0;
            state.wasGliding = false;
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private void handleElytraMovement(WindfallPlayer player, PlayerState state) {
        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();
        double deltaY = player.getDeltaY();

        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (horizontalSpeed > ELYTRA_MAX_HORIZONTAL_SPEED) {
            SimulationEngine simEngine = WindfallMod.getInstance().getSimulationEngine();
            if (simEngine != null && simEngine.needsSimulation(player)) {
                SimulationEngine.SimulationResult result = simEngine.simulate(
                    player, player.getLastX() + deltaX, player.getY(), player.getLastZ() + deltaZ);
                if (result.matches) {
                    decreaseBuffer(player, 0.2);
                    state.lastElytraDeltaY = deltaY;
                    return;
                }
            }

            double ratio = horizontalSpeed / ELYTRA_MAX_HORIZONTAL_SPEED;
            if (ratio > 2.0) {
                flag(player);
                resetBuffer(player);
                return;
            }
            increaseBuffer(player, 0.5 * (ratio - 1.0));
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }

        if (Math.abs(deltaY) < ELYTRA_HOVER_DELTA) {
            state.elytraHoverTicks++;
            if (state.elytraHoverTicks > ELYTRA_HOVER_TICK_THRESHOLD) {
                increaseBuffer(player, 0.8);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                    state.elytraHoverTicks = 0;
                }
            }
        } else {
            state.elytraHoverTicks = Math.max(0, state.elytraHoverTicks - 1);
        }

        if (deltaY > 0 && deltaY > ELYTRA_KICKBOOST_MAX && player.isOnGround()) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        }

        if (deltaY > 0 && !player.isOnGround() && state.elytraTicks > 5) {
            double expectedDescent = ELYTRA_MIN_DESCENT;
            if (deltaY > Math.abs(expectedDescent) + ELYTRA_VERTICAL_TOLERANCE) {
                increaseBuffer(player, 0.4);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        state.lastElytraDeltaY = deltaY;
    }

    private void handleElytraLanding(WindfallPlayer player, PlayerState state) {
        if (state.elytraTicks < 5) return;
    }
}
