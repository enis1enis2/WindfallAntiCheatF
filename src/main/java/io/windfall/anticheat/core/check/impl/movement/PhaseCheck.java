package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.compensation.SimulationEngine;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects phase/noclip — clients that move inside or through solid blocks.
 *
 * <p>Algorithm: Each movement packet, the player's feet position and head position (1.6 blocks above
 * feet) are checked against the world block grid. If either position is inside a solid block, the
 * clipping tick counter increments. After {@value MIN_CLIPPING_TICKS} consecutive ticks inside a
 * solid block, the check verifies the player is still moving (horizontal or vertical speed &gt;
 * {@value MAX_VELOCITY_INSIDE_BLOCK}). Movement while clipping is heavily penalized because
 * legitimate players cannot move inside solid blocks.
 *
 * @see FlightCheck for vertical movement abuse
 * @see SpeedCheck for horizontal speed validation
 */
@CheckData(name = "Phase A", stableKey = "windfall.movement.phase", decay = 0.01, setbackVl = 20, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.4)
public class PhaseCheck extends Check implements PacketCheck {

    private static final double MAX_VELOCITY_INSIDE_BLOCK = 0.01;
    private static final int MIN_CLIPPING_TICKS = 3;

    private static final class PlayerState {
        int clippingTicks;
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

        PlayerState state = getState(player);
        PredictionContext ctx = new PredictionContext(player);

        try {
            ServerWorld world = (ServerWorld) player.getServerPlayer().getWorld();
            BlockPos feetPos = new BlockPos(
                (int) Math.floor(ctx.x), (int) Math.floor(ctx.y), (int) Math.floor(ctx.z));
            BlockPos headPos = new BlockPos(
                (int) Math.floor(ctx.x), (int) Math.floor(ctx.y + 1.6), (int) Math.floor(ctx.z));

            boolean feetInside = world.getBlockState(feetPos).isSolidBlock(world, feetPos);
            boolean headInside = world.getBlockState(headPos).isSolidBlock(world, headPos);

            if (feetInside || headInside) {
                SimulationEngine simEngine = WindfallMod.getInstance().getSimulationEngine();
                if (simEngine != null && simEngine.needsSimulation(player)) {
                    SimulationEngine.SimulationResult result = simEngine.simulate(
                        player, ctx.x, ctx.y, ctx.z);
                    if (result.matches) {
                        state.clippingTicks = Math.max(0, state.clippingTicks - 1);
                        decreaseBuffer(player, 0.3);
                        return;
                    }
                }

                state.clippingTicks++;
                if (state.clippingTicks >= MIN_CLIPPING_TICKS) {
                    if (ctx.horizontalSpeed > MAX_VELOCITY_INSIDE_BLOCK
                            || Math.abs(ctx.deltaY) > MAX_VELOCITY_INSIDE_BLOCK) {
                        increaseBuffer(player, 1.5);
                        if (getBuffer(player) > 3.0) {
                            flag(player);
                            resetBuffer(player);
                            state.clippingTicks = 0;
                        }
                    }
                }
            } else {
                state.clippingTicks = Math.max(0, state.clippingTicks - 1);
                decreaseBuffer(player, 0.2);
            }
        } catch (Exception e) {
            WindfallMod.LOGGER.debug("PhaseCheck: chunk-load or world-access exception — " + e.getMessage());
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
