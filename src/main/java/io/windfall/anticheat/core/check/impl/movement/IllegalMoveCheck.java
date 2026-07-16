package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import io.windfall.anticheat.core.player.data.ActionData;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.physics.PredictionEngine;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects players moving at non-sprint speeds while not sprinting (sprint disabler).
 *
 * <p>This check catches clients that disable the sprint packet to mask horizontal speed
 * while still moving at sprint-level velocities. When a player has not sprinted recently
 * and is not receiving velocity, their horizontal speed should match non-sprint movement
 * limits. Exceeding these limits indicates the client is suppressing sprint state while
 * still benefiting from sprint speed.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Track sprint history (last and last-last sprint state)</li>
 *   <li>If player has not sprinted for >20 ticks, no active velocity, not in vehicle, not near wall:
 *       check horizontal speed against ground/air limits</li>
 *   <li>Ground limit: {@code 0.24 * frictionMultiplier}</li>
 *   <li>Air limit: {@code 0.221 * frictionMultiplier}</li>
 *   <li>Speed limit adjusted by velocity contribution, potion effects, and collision state</li>
 *   <li>Buffer builds gradually; flag when buffer exceeds {@value MAX_BUFFER}</li>
 * </ol>
 *
 * @see SpeedCheck for general horizontal speed detection
 * @see FlightCheck for vertical movement detection
 */
@CheckData(name = "IllegalMove A", stableKey = "windfall.movement.illegalmove", decay = 0.04, setbackVl = 10, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class IllegalMoveCheck extends Check implements PacketCheck {

    private static final double BASE_AIR_LIMIT = 0.221;
    private static final double BASE_GROUND_LIMIT = 0.24;
    private static final double MAX_BUFFER = 13.0;
    private static final double RESET_RATE = 0.4;

    private static final class PlayerState {
        boolean lastSprinting;
        boolean lastLastSprinting;
        int sinceLastSprintingTicks;
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

        boolean inWater = PredictionEngine.checkInWater(player);
        boolean inLava = PredictionEngine.checkInLava(player);
        if (inWater || inLava) {
            resetBuffer(player);
            return;
        }

        PlayerState state = getState(player);

        boolean isSprinting = player.isSprinting();
        boolean lastSprinting = state.lastSprinting;
        boolean lastLastSprinting = state.lastLastSprinting;

        if (isSprinting) {
            state.sinceLastSprintingTicks = 0;
        } else {
            state.sinceLastSprintingTicks++;
        }

        PredictionContext ctx = new PredictionContext(player);
        double deltaXZ = ctx.horizontalSpeed;
        boolean isOnGround = ctx.onGround;
        boolean isLastOnGround = ctx.lastOnGround;

        double blockFriction = 0.91;
        if (isOnGround) {
            blockFriction = getBlockFriction(player);
        }

        double frictionMultiplier = blockFriction;
        if (ctx.baseSpeed <= 0.13 && PredictionEngine.getSpeedPotionMultiplier(player) <= 1.0) {
            frictionMultiplier = blockFriction;
        } else {
            frictionMultiplier = Math.max(0.91, blockFriction);
        }

        double airLimit = BASE_AIR_LIMIT * frictionMultiplier;
        double groundLimit = BASE_GROUND_LIMIT * frictionMultiplier;

        double speedPotionBonus = 0;
        if (PredictionEngine.getSpeedPotionMultiplier(player) > 1.0) {
            speedPotionBonus = 0.05 + (0.01 * (PredictionEngine.getSpeedPotionMultiplier(player) - 1.0) / 0.2);
        }
        airLimit += speedPotionBonus * frictionMultiplier;
        groundLimit += speedPotionBonus * frictionMultiplier;

        double velocityContribution = Math.max(
                Math.abs(player.getServerVelocityX()) + Math.abs(player.getServerVelocityZ()), 0);
        airLimit += velocityContribution + 0.05;
        groundLimit += velocityContribution + 0.05;

        SimulationEngine simEngine = WindfallMod.getInstance().getSimulationEngine();
        boolean unconfirmedChanges = simEngine != null && simEngine.needsSimulation(player);
        if (unconfirmedChanges) {
            airLimit += 0.08;
            groundLimit += 0.08;
        }

        double deltaXZWF = deltaXZ * blockFriction;

        boolean velocityActive = player.getServerVelocityX() != 0
                || player.getServerVelocityY() != 0
                || player.getServerVelocityZ() != 0;

        boolean basicInvalidState = !isSprinting
                && !lastSprinting
                && !lastLastSprinting
                && !velocityActive
                && state.sinceLastSprintingTicks > 20;

        if (isOnGround && isLastOnGround && deltaXZWF > groundLimit && basicInvalidState) {
            if (increaseBufferAndCheck(player, 1.0)) {
                flag(player);
                resetBuffer(player);
                increaseBuffer(player, MAX_BUFFER + 2);
            }
        } else if (!isOnGround && !isLastOnGround && deltaXZWF > airLimit && basicInvalidState) {
            if (increaseBufferAndCheck(player, 1.0)) {
                flag(player);
                resetBuffer(player);
                increaseBuffer(player, MAX_BUFFER + 2);
            }
        } else {
            decreaseBuffer(player, RESET_RATE);
        }

        state.lastLastSprinting = lastSprinting;
        state.lastSprinting = isSprinting;
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private double getBlockFriction(WindfallPlayer player) {
        try {
            ServerPlayerEntity sp = player.getServerPlayer();
            if (sp == null) return PhysicsConstants.GROUND_FRICTION;
            BlockPos blockPos = sp.getBlockPos().down();
            net.minecraft.block.Block block = sp.getWorld().getBlockState(blockPos).getBlock();
            return PhysicsConstants.getBlockFriction(block);
        } catch (Exception e) {
            return PhysicsConstants.GROUND_FRICTION;
        }
    }

    private boolean increaseBufferAndCheck(WindfallPlayer player, double amount) {
        increaseBuffer(player, amount);
        return getBuffer(player) > MAX_BUFFER;
    }
}
