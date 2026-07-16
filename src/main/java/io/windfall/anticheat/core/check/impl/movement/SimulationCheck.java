package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import io.windfall.anticheat.core.physics.PhysicsConstants;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.version.VersionBracket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects movement simulation mismatches — clients whose vertical movement does not match
 * vanilla Minecraft's gravity + drag physics model.
 *
 * <p>Algorithm: Tracks a running expected vertical velocity ({@code expectedDeltaY}) and compares
 * it against the player's actual deltaY each tick. The prediction applies:
 * <ul>
 *   <li><b>Airborne</b>: {@code predicted = (expectedDeltaY - GRAVITY) × AIR_DRAG}</li>
 *   <li><b>Swimming</b>: {@code predicted = (expectedDeltaY - GRAVITY) × WATER_DRAG}</li>
 * </ul>
 *
 * <p>The absolute deviation between predicted and actual deltaY is compared against
 * {@value MAX_SIMULATION_DEVIATION} blocks. Horizontal movement must exceed 0.1 blocks to
 * avoid false positives from near-stationary floating-point imprecision.
 *
 * <p>Tolerance is widened for:
 * <ul>
 *   <li>Bedrock clients: +15% (different physics precision)</li>
 *   <li>Legacy/Combat protocol versions: +20% (older movement rounding)</li>
 * </ul>
 *
 * <p>After {@value MIN_SAMPLES} consecutive deviations, the buffer builds proportionally to the
 * deviation ratio.
 *
 * @see PhysicsConstants for GRAVITY and AIR_DRAG values
 * @see FlightCheck for a complementary hover/flight detection
 * @see SpeedCheck for horizontal speed validation
 */
@CheckData(name = "Simulation A", stableKey = "windfall.movement.simulation", decay = 0.01, setbackVl = 20,
    compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.4)
public class SimulationCheck extends Check implements PacketCheck {

    private static final double MAX_SIMULATION_DEVIATION = 0.15;
    private static final int MIN_SAMPLES = 10;

    private static final class PlayerState {
        double expectedDeltaY;
        int samples;
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
        boolean onGround = player.isOnGround();
        double deltaY = player.getDeltaY();
        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();

        if (onGround) {
            state.expectedDeltaY = 0;
            state.samples = 0;
            return;
        }

        int protocol = player.getProtocolVersion();
        double gravity = PhysicsConstants.GRAVITY;
        double airDrag = PhysicsConstants.AIR_DRAG;

        if (player.isSwimming()) {
            double waterDrag = protocol >= 393 ? PhysicsConstants.WATER_DRAG : 0.8;
            double predictedDeltaY = (state.expectedDeltaY - gravity) * waterDrag;
            double verticalDeviation = Math.abs(deltaY - predictedDeltaY);
            checkDeviation(player, verticalDeviation, deltaX, deltaZ, state);
            state.expectedDeltaY = deltaY;
            return;
        }

        double predictedDeltaY = (state.expectedDeltaY - gravity) * airDrag;
        double verticalDeviation = Math.abs(deltaY - predictedDeltaY);

        SimulationEngine simEngine = WindfallMod.getInstance().getSimulationEngine();
        if (simEngine != null && simEngine.needsSimulation(player)) {
            SimulationEngine.SimulationResult result = simEngine.simulate(player,
                player.getLastX() + player.getDeltaX(),
                player.getLastY() + deltaY,
                player.getLastZ() + player.getDeltaZ());
            if (result.matches) {
                state.samples = Math.max(0, state.samples - 2);
                decreaseBuffer(player, 0.2);
                state.expectedDeltaY = deltaY;
                return;
            }
        }

        checkDeviation(player, verticalDeviation, deltaX, deltaZ, state);
        state.expectedDeltaY = deltaY;
    }

    private void checkDeviation(WindfallPlayer player, double verticalDeviation, double deltaX, double deltaZ, PlayerState state) {
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        double maxDeviation = MAX_SIMULATION_DEVIATION;
        io.windfall.anticheat.core.bedrock.GeyserManager geyser = WindfallMod.getInstance().getGeyserManager();
        if (geyser != null && geyser.isBedrockPlayer(player.getUuid())) {
            maxDeviation *= 1.15;
        }
        int protocol = player.getProtocolVersion();
        VersionBracket bracket = VersionBracket.fromProtocol(protocol);
        if (bracket == VersionBracket.LEGACY || bracket == VersionBracket.OLD) {
            maxDeviation *= 1.2;
        }

        if (verticalDeviation > maxDeviation && horizontalSpeed > 0.1) {
            state.samples++;
            if (state.samples >= MIN_SAMPLES) {
                increaseBuffer(player, 0.5 * (verticalDeviation / maxDeviation));
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                    state.samples = 0;
                }
            }
        } else {
            state.samples = Math.max(0, state.samples - 1);
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
