package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.concurrent.ConcurrentHashMap;

@CheckData(name="Simulation A", stableKey="windfall.movement.simulation", decay=0.01, setbackVl=15)
public class SimulationCheck extends Check implements PacketCheck {

    private static final double DEVIATION_THRESHOLD = 0.5;
    private static final double GRAVITY = 0.08;
    private static final double AIR_DRAG = 0.98;
    private static final double WATER_DRAG = 0.8;
    private static final double CLIMB_SPEED = 0.115;

    private final ConcurrentHashMap<String, PlayerState> playerStates = new ConcurrentHashMap<>();

    static final class PlayerState {
        double lastX, lastY, lastZ;
        double lastDeltaY;
        boolean wasOnGround;
        boolean wasInWater;
        boolean wasClimbing;
        boolean wasFlying;
        boolean wasGliding;
        int ticksSinceFlag;

        PlayerState() {}
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;

        PlayerState state = playerStates.computeIfAbsent(player.getUuid().toString(), k -> new PlayerState());

        if (state.ticksSinceFlag > 0) state.ticksSinceFlag--;

        double currentX = player.getX();
        double currentY = player.getY();
        double currentZ = player.getZ();

        if (state.lastX == 0 && state.lastY == 0 && state.lastZ == 0) {
            state.lastX = currentX;
            state.lastY = currentY;
            state.lastZ = currentZ;
            state.lastDeltaY = 0;
            state.wasOnGround = player.isOnGround();
            return;
        }

        double actualDeltaX = currentX - state.lastX;
        double actualDeltaY = currentY - state.lastY;
        double actualDeltaZ = currentZ - state.lastZ;

        double predictedDeltaY;
        double predictedDeltaX = 0;
        double predictedDeltaZ = 0;

        if (player.isOnGround()) {
            predictedDeltaY = 0;
            predictedDeltaX = actualDeltaX;
            predictedDeltaZ = actualDeltaZ;
        } else if (state.wasInWater || player.isCachedInWater()) {
            double waterMotionY = state.lastDeltaY * WATER_DRAG;
            predictedDeltaY = (waterMotionY - GRAVITY * 0.02) * WATER_DRAG;
            predictedDeltaX = actualDeltaX * WATER_DRAG;
            predictedDeltaZ = actualDeltaZ * WATER_DRAG;
        } else if (state.wasClimbing || player.isClimbing()) {
            predictedDeltaY = Math.max(actualDeltaY, CLIMB_SPEED);
            predictedDeltaX = actualDeltaX;
            predictedDeltaZ = actualDeltaZ;
        } else {
            double drag = player.isSprinting() ? 0.96 : AIR_DRAG;
            predictedDeltaY = (state.lastDeltaY - GRAVITY) * drag;
            predictedDeltaX = actualDeltaX;
            predictedDeltaZ = actualDeltaZ;
        }

        if (state.wasOnGround && actualDeltaY > 0 && actualDeltaY < 0.425 && actualDeltaY > 0.0) {
            predictedDeltaY = actualDeltaY;
        }

        double deltaYDeviation = Math.abs(actualDeltaY - predictedDeltaY);
        double deltaXDeviation = Math.abs(actualDeltaX - predictedDeltaX);
        double deltaZDeviation = Math.abs(actualDeltaZ - predictedDeltaZ);
        double totalDeviation = Math.sqrt(deltaXDeviation * deltaXDeviation + deltaYDeviation * deltaYDeviation + deltaZDeviation * deltaZDeviation);

        if (totalDeviation > DEVIATION_THRESHOLD && state.ticksSinceFlag < 1) {
            increaseBuffer(player, (totalDeviation - DEVIATION_THRESHOLD) * 2.0);
            if (getBuffer(player) > 2.0) {
                flag(player);
                resetBuffer(player);
                state.ticksSinceFlag = 20;
            }
        } else {
            decreaseBuffer(player, 0.15);
        }

        state.lastX = currentX;
        state.lastY = currentY;
        state.lastZ = currentZ;
        state.lastDeltaY = actualDeltaY;
        state.wasOnGround = player.isOnGround();
        state.wasInWater = player.isCachedInWater();
        state.wasClimbing = player.isClimbing();
        state.wasFlying = player.isFlying();
        state.wasGliding = player.isGliding();
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    @Override
    public void removePlayer(java.util.UUID uuid) {
        playerStates.remove(uuid.toString());
    }
}
