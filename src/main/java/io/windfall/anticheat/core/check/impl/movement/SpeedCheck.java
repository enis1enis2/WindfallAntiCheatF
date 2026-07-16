package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects horizontal speed exceeding the maximum predicted movement for the player's current state.
 *
 * <p>Algorithm: Each movement packet, a {@link PredictionContext} computes the server-authoritative
 * maximum horizontal speed ({@link PredictionContext#predictedMaxHorizontalSpeed}) accounting for
 * sprint, potions, ice, soul sand, cobwebs, and other movement modifiers. The player's actual
 * horizontal speed is compared against this maximum with a small tolerance margin.
 *
 * <p>Detection stages:
 * <ol>
 *   <li>If actual speed exceeds predicted max by more than {@value SPEED_TOLERANCE}x, the buffer
 *       increases proportionally to the exceed ratio.</li>
 *   <li>If the exceed ratio exceeds 2.0, the player is flagged immediately (blatant hack).</li>
 *   <li>If the buffer exceeds {@value MIN_SPEED_FLAG_BUFFER}, the player is flagged (gradual buildup).</li>
 * </ol>
 *
 * <p>Special cases:
 * <ul>
 *   <li>Pre-1.18.2 clients may report sub-threshold speeds due to protocol differences — these are
 *       excluded via {@value PRE_1_18_2_THRESHOLD}.</li>
 *   <li>Near-zero speeds (&lt;0.005) are ignored to avoid floating-point noise.</li>
 * </ul>
 *
 * @see PredictionContext for speed prediction logic
 * @see PredictionEngine for movement packet detection
 */
@CheckData(name = "Speed A", stableKey = "windfall.movement.speed", decay = 0.01, setbackVl = 20, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.5)
public class SpeedCheck extends Check implements PacketCheck {

    /** Multiplier applied to the predicted max speed before flagging — 1.05 allows 5% headroom for float rounding */
    private static final double SPEED_TOLERANCE = 1.05;
    /** Widened tolerance when unconfirmed state changes exist — prevents false positives from latency-delayed world changes */
    private static final double SPEED_TOLERANCE_UNCONFIRMED = 1.20;
    /** Minimum horizontal speed on pre-1.18.2 clients; smaller values indicate protocol-version-specific float compression artifacts */
    private static final double PRE_1_18_2_THRESHOLD = 0.03;
    /** Buffer level at which a gradual speed violation triggers a flag */
    private static final double MIN_SPEED_FLAG_BUFFER = 3.0;

    private static final class PlayerState {
        double maxObservedSpeed;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(WindfallPlayer player) {
        return stateMap.computeIfAbsent(player.getUuid(), k -> new PlayerState());
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;

        io.windfall.anticheat.core.player.data.ActionData actionData = player.getActionData();

        if (actionData.hasRecentPistonUpdate(5)) {
            decreaseBuffer(player, 0.5);
            return;
        }

        if (actionData.hasRecentBlockUpdateUnder(5)) {
            decreaseBuffer(player, 0.3);
            return;
        }

        PredictionContext ctx = new PredictionContext(player);

        double actualSpeed = ctx.horizontalSpeed;

        PlayerState state = getState(player);
        if (actualSpeed > state.maxObservedSpeed) {
            state.maxObservedSpeed = actualSpeed;
        }

        if (actualSpeed < PRE_1_18_2_THRESHOLD && ctx.protocolVersion < 757) {
            decreaseBuffer(player, 0.1);
            return;
        }

        if (actualSpeed < 0.005) {
            decreaseBuffer(player, 0.05);
            return;
        }

        double maxSpeed = ctx.predictedMaxHorizontalSpeed;

        SimulationEngine simEngine = WindfallMod.getInstance().getSimulationEngine();
        boolean unconfirmedChanges = simEngine != null && simEngine.needsSimulation(player);
        double tolerance = unconfirmedChanges ? SPEED_TOLERANCE_UNCONFIRMED : SPEED_TOLERANCE;

        if (actualSpeed > maxSpeed * tolerance) {
            double exceedRatio = actualSpeed / Math.max(maxSpeed, 0.001);
            if (exceedRatio > 2.0) {
                flag(player);
                resetBuffer(player);
            } else {
                increaseBuffer(player, 0.5 * (exceedRatio - 1.0));
                if (getBuffer(player) > MIN_SPEED_FLAG_BUFFER) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public double getMaxObservedSpeed(WindfallPlayer player) {
        return getState(player).maxObservedSpeed;
    }
}
