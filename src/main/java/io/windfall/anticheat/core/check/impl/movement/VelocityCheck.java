package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.version.VersionBracket;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Detects knockback (velocity) rejection — clients that receive an entity velocity packet but
 * fail to reflect the expected movement in their subsequent position updates.
 *
 * <p>Algorithm:
 * <ol>
 *   <li><b>Capture</b>: When the server sends an {@code ENTITY_VELOCITY} packet to the player,
 *       the raw velocity vector is decoded (divided by 8000.0 as per the MC protocol) and queued.</li>
 *   <li><b>Apply physics</b>: On the next movement packet, the pending velocity is consumed and
 *       run through {@link #applyPostVelocityPhysics} which applies ground friction,
 *       air drag, water drag, climbing slowdown.</li>
 *   <li><b>Compare</b>: The expected post-physics horizontal and vertical distances are compared
 *       to the player's actual deltas. The combined ratio (average of horizontal and vertical)
 *       indicates how much of the knockback was honored.</li>
 * </ol>
 *
 * <p>Detection thresholds (adjusted for legacy versions and nearby walls):
 * <ul>
 *   <li>{@code combinedRatio &lt; 0.5 / tolerance} → blatant rejection, high buffer increase</li>
 *   <li>{@code combinedRatio &lt; 0.8 / tolerance} → partial rejection, gradual buffer increase</li>
 *   <li>{@code combinedRatio ≥ 0.8 / tolerance} → legitimate, buffer decreases</li>
 * </ul>
 *
 * <p>Tolerance is widened for legacy protocol versions (+20%) and when the player is near a solid
 * wall (+30%) to avoid false positives from legitimate block-collision knockback absorption.
 *
 * @see PhysicsConstants for vanilla friction/drag values
 */
@CheckData(name = "Velocity A", stableKey = "windfall.movement.velocity", decay = 0.01, setbackVl = 30,
    compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class VelocityCheck extends Check implements PacketCheck {

    /** Minimum velocity magnitude to consider — filters out negligible knockback (noise threshold) */
    private static final double MIN_VELOCITY_THRESHOLD = 0.005;
    /** Maximum queued velocity packets per player — prevents memory abuse from packet spam */
    private static final int MAX_PENDING_VELOCITIES = 5;

    private static final class PlayerState {
        final ConcurrentLinkedDeque<PendingVelocity> pendingVelocities = new ConcurrentLinkedDeque<>();
        boolean velocityActive;
        double expectedDeltaX;
        double expectedDeltaY;
        double expectedDeltaZ;
        int velocityAge;
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
    public void onPacketSend(WindfallPlayer player, Object packet) {
        if (!(packet instanceof EntityVelocityUpdateS2CPacket)) return;

        EntityVelocityUpdateS2CPacket velPacket = (EntityVelocityUpdateS2CPacket) packet;
        if (velPacket.getEntityId() != player.getServerPlayer().getId()) return;

        double velX = velPacket.getVelocityX();
        double velY = velPacket.getVelocityY();
        double velZ = velPacket.getVelocityZ();

        if (Math.abs(velX) < MIN_VELOCITY_THRESHOLD
                && Math.abs(velY) < MIN_VELOCITY_THRESHOLD
                && Math.abs(velZ) < MIN_VELOCITY_THRESHOLD) {
            return;
        }

        PlayerState state = getState(player);
        while (state.pendingVelocities.size() >= MAX_PENDING_VELOCITIES) {
            state.pendingVelocities.removeFirst();
        }
        state.pendingVelocities.addLast(new PendingVelocity(velX, velY, velZ, System.currentTimeMillis()));
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;

        PlayerState state = getState(player);

        if (!state.velocityActive) {
            PendingVelocity pv;
            while ((pv = state.pendingVelocities.peekFirst()) != null) {
                long age = System.currentTimeMillis() - pv.receivedAt;
                long timeout = 500L + player.getTransactionPing();
                if (age > timeout) {
                    state.pendingVelocities.pollFirst();
                    continue;
                }
                state.pendingVelocities.pollFirst();
                state.velocityActive = true;

                double[] postDrag = applyPostVelocityPhysics(pv.velX, pv.velY, pv.velZ, player);
                state.expectedDeltaX = postDrag[0];
                state.expectedDeltaY = postDrag[1];
                state.expectedDeltaZ = postDrag[2];
                state.velocityAge = 0;
                return;
            }
            return;
        }

        if (!state.velocityActive) return;

        state.velocityAge++;

        double actualDeltaX = player.getDeltaX();
        double actualDeltaZ = player.getDeltaZ();
        double actualDeltaY = player.getDeltaY();

        double expectedHorizontalDist = Math.sqrt(state.expectedDeltaX * state.expectedDeltaX + state.expectedDeltaZ * state.expectedDeltaZ);
        double actualHorizontalDist = Math.sqrt(actualDeltaX * actualDeltaX + actualDeltaZ * actualDeltaZ);

        if (expectedHorizontalDist < MIN_VELOCITY_THRESHOLD && Math.abs(state.expectedDeltaY) < MIN_VELOCITY_THRESHOLD) {
            state.velocityActive = false;
            resetBuffer(player);
            return;
        }

        double horizontalRatio;
        if (expectedHorizontalDist > MIN_VELOCITY_THRESHOLD) {
            horizontalRatio = actualHorizontalDist / expectedHorizontalDist;
        } else {
            horizontalRatio = actualHorizontalDist < MIN_VELOCITY_THRESHOLD ? 1.0 : 0.0;
        }

        double verticalRatio;
        if (Math.abs(state.expectedDeltaY) > MIN_VELOCITY_THRESHOLD) {
            verticalRatio = Math.abs(actualDeltaY) / Math.abs(state.expectedDeltaY);
        } else {
            verticalRatio = Math.abs(actualDeltaY) < MIN_VELOCITY_THRESHOLD ? 1.0 : 0.0;
        }

        double combinedRatio = (horizontalRatio + verticalRatio) / 2.0;

        state.velocityActive = false;

        if (state.velocityAge > 5) {
            resetBuffer(player);
            return;
        }

        double tolerance = 1.0;

        int protocol = player.getProtocolVersion();
        VersionBracket bracket = VersionBracket.fromProtocol(protocol);
        if (bracket == VersionBracket.LEGACY) {
            tolerance *= 1.2;
        }

        if (isNearWall(player)) {
            tolerance *= 1.3;
        }

        double adjustedThreshold05 = 0.5 / tolerance;
        double adjustedThreshold08 = 0.8 / tolerance;

        if (combinedRatio < adjustedThreshold05) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (combinedRatio < adjustedThreshold08) {
            increaseBuffer(player, 0.3);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.5);
        }
    }

    private double[] applyPostVelocityPhysics(double velX, double velY, double velZ, WindfallPlayer player) {
        double groundFriction = PhysicsConstants.GROUND_FRICTION;
        double gravity = PhysicsConstants.GRAVITY;
        double airDrag = PhysicsConstants.AIR_DRAG;

        double newDeltaX = velX * groundFriction;
        double newDeltaZ = velZ * groundFriction;

        double newDeltaY;
        if (player.isSwimming()) {
            double waterDrag = player.getProtocolVersion() >= 393 ? PhysicsConstants.WATER_DRAG : 0.8;
            newDeltaY = velY * waterDrag - 0.02;
        } else if (player.isClimbing()) {
            newDeltaY = Math.max(velY, -0.15);
            newDeltaX *= 0.2;
            newDeltaZ *= 0.2;
        } else {
            newDeltaY = (velY - gravity) * airDrag;
        }

        if (!player.isOnGround()) {
            double airAcceleration = player.getHorizontalSpeed() * 0.026;
            double maxPostVelHorizontal = Math.sqrt(newDeltaX * newDeltaX + newDeltaZ * newDeltaZ);
            if (maxPostVelHorizontal > MIN_VELOCITY_THRESHOLD) {
                double accelContribution = Math.min(airAcceleration, maxPostVelHorizontal * 0.1);
                newDeltaX += (player.getDeltaX() > 0 ? 1 : -1) * accelContribution;
                newDeltaZ += (player.getDeltaZ() > 0 ? 1 : -1) * accelContribution;
            }
        }

        return new double[]{newDeltaX, newDeltaY, newDeltaZ};
    }

    private boolean isNearWall(WindfallPlayer player) {
        ServerPlayerEntity sp = player.getServerPlayer();
        if (sp == null) return false;
        ServerWorld world = (ServerWorld) sp.getWorld();
        int px = (int) Math.floor(player.getX());
        int py = (int) Math.floor(player.getY());
        int pz = (int) Math.floor(player.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos p1 = new BlockPos(px + dx, py - 1, pz + dz);
                BlockPos p2 = new BlockPos(px + dx, py, pz + dz);
                BlockPos p3 = new BlockPos(px + dx, py + 1, pz + dz);
                if (world.getBlockState(p1).isSolidBlock(world, p1)) return true;
                if (world.getBlockState(p2).isSolidBlock(world, p2)) return true;
                if (world.getBlockState(p3).isSolidBlock(world, p3)) return true;
            }
        }
        return false;
    }

    private static final class PendingVelocity {
        final double velX, velY, velZ;
        final long receivedAt;

        PendingVelocity(double velX, double velY, double velZ, long receivedAt) {
            this.velX = velX;
            this.velY = velY;
            this.velZ = velZ;
            this.receivedAt = receivedAt;
        }
    }
}
