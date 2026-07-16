package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import io.windfall.anticheat.core.player.data.ActionData;
import io.windfall.anticheat.core.physics.PredictionEngine;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects vertical movement that deviates from the expected Minecraft gravity cycle.
 *
 * <p>Unlike {@link FlightCheck} which uses a full physics prediction model, this check
 * validates that the player's vertical velocity follows the strict gravity formula:
 * {@code nextDeltaY = (currentDeltaY - 0.08) * 0.98} (normal air), or
 * {@code nextDeltaY = (currentDeltaY * 0.8) + (0.01 * levitationAmplifier)} (levitation).
 *
 * <p>This catches gravity-defying hacks that bypass other flight checks by subtly
 * manipulating vertical velocity within the tolerance of more complex prediction models.
 *
 * <p>Detection algorithm:
 * <ol>
 *   <li>Predict next-tick deltaY using the vanilla gravity formula</li>
 *   <li>If the player is not on ground, not gliding, not using riptide, and not in fluid:
 *       compare actual deltaY against predicted deltaY</li>
 *   <li>Upward deviation when expected to fall is penalized more heavily</li>
 *   <li>Buffer builds gradually; flag when buffer exceeds threshold</li>
 * </ol>
 *
 * @see FlightCheck for the comprehensive flight detection
 * @see PredictionEngine for the physics prediction engine
 */
@CheckData(name = "Gravity A", stableKey = "windfall.movement.gravity", decay = 0.01, setbackVl = 8, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class GravityCheck extends Check implements PacketCheck {

    private static final double GRAVITY = 0.08;
    private static final double AIR_DRAG = 0.98;
    private static final double GRAVITY_TOLERANCE = 0.05;
    private static final double GRAVITY_TOLERANCE_UNCONFIRMED = 0.12;
    private static final double FLAG_BUFFER = 5.0;

    private static final class PlayerState {
        double lastDeltaY;
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

        if (player.isGliding() || player.isFlying()) return;

        boolean hasRiptide = PredictionEngine.checkRiptiding(player);
        boolean isFallFlying = PredictionEngine.checkFallFlying(player);
        if (hasRiptide || isFallFlying) return;

        ActionData actionData = player.getActionData();

        if (actionData.hasRecentPistonUpdate(5)) {
            decreaseBuffer(player, 0.5);
            return;
        }

        if (actionData.hasRecentBlockUpdateUnder(5)) {
            decreaseBuffer(player, 0.3);
            return;
        }

        PlayerState state = getState(player);

        double deltaY = player.getDeltaY();
        boolean onGround = player.isOnGround();

        if (onGround) {
            state.lastDeltaY = 0;
            return;
        }

        boolean inWater = PredictionEngine.checkInWater(player);
        boolean inLava = PredictionEngine.checkInLava(player);
        boolean climbing = player.isClimbing();
        boolean onHoney = PredictionEngine.checkOnHoney(player);
        boolean hasSlowFalling = PredictionEngine.checkSlowFalling(player);
        boolean hasLevitation = PredictionEngine.checkLevitation(player);

        if (inWater || inLava || climbing || onHoney) {
            state.lastDeltaY = deltaY;
            return;
        }

        SimulationEngine simEngine = WindfallMod.getInstance().getSimulationEngine();
        boolean unconfirmedChanges = simEngine != null && simEngine.needsSimulation(player);
        double tolerance = unconfirmedChanges ? GRAVITY_TOLERANCE_UNCONFIRMED : GRAVITY_TOLERANCE;

        double predictedDeltaY;
        if (hasLevitation) {
            double amplifier = PredictionEngine.getLevitationAmplifier(player);
            predictedDeltaY = (state.lastDeltaY * 0.8) + (0.01 * amplifier);
        } else {
            double gravity = hasSlowFalling ? 0.01 : GRAVITY;
            predictedDeltaY = (state.lastDeltaY - gravity) * AIR_DRAG;
        }

        double deviation = Math.abs(deltaY - predictedDeltaY);

        if (deviation > tolerance && Math.abs(deltaY) > 0.01) {
            if (deltaY > 0 && state.lastDeltaY <= 0 && !hasLevitation) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > FLAG_BUFFER) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                double deviationRatio = deviation / Math.max(Math.abs(predictedDeltaY), 0.001);
                if (deviationRatio > 2.0) {
                    flag(player);
                    resetBuffer(player);
                } else {
                    increaseBuffer(player, 0.3 * Math.min(deviationRatio, 2.0));
                    if (getBuffer(player) > FLAG_BUFFER) {
                        flag(player);
                        resetBuffer(player);
                    }
                }
            }
        } else {
            decreaseBuffer(player, 0.1);
        }

        state.lastDeltaY = deltaY;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
