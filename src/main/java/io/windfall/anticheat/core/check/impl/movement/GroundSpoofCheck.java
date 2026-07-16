package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.physics.PredictionContext;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects clients that falsely claim to be on the ground (ground spoofing) to manipulate knockback,
 * friction, or bypass fall-damage calculations.
 *
 * <p>Algorithm: The check tracks two conditions independently:
 * <ol>
 *   <li><b>Falling + on-ground</b>: If the player reports on-ground while falling faster than
 *       {@value FALLING_VELOCITY_THRESHOLD} blocks/tick with more than {@value MIN_FALL_DISTANCE}
 *       blocks of fall distance, the false-ground counter increments. After
 *       {@value MIN_FALSE_GROUND_FLAGS} consecutive violations, the check flags immediately.</li>
 *   <li><b>Air-to-ground snap</b>: If the player is airborne for more than
 *       {@value MIN_AIR_TIME_FOR_GROUND} seconds ({@value MIN_AIR_TIME_FOR_GROUND * TICKS_PER_SECOND}
 *       ticks) and suddenly claims on-ground with significant vertical velocity (|deltaY| &gt; 0.5),
 *       the buffer increases — catches teleports to ground after extended flight.</li>
 * </ol>
 *
 * @see PredictionContext for ground-truth state
 * @see FlightCheck for complementary flight detection
 */
@CheckData(name = "Ground Spoof A", stableKey = "windfall.movement.groundspoof", decay = 0.01, setbackVl = 20, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class GroundSpoofCheck extends Check implements PacketCheck {

    /** Minimum seconds a player must be airborne before a ground claim after long flight is suspicious */
    private static final double MIN_AIR_TIME_FOR_GROUND = 3.0;
    /** Standard Minecraft tick rate — used to convert seconds to tick count */
    private static final int TICKS_PER_SECOND = 20;
    /** Number of consecutive falling-while-on-ground violations before the check flags */
    private static final int MIN_FALSE_GROUND_FLAGS = 5;
    /** Minimum downward velocity (blocks/tick) to consider the player as actually falling */
    private static final double FALLING_VELOCITY_THRESHOLD = 0.3;
    /** Minimum fall distance (blocks) to validate the falling+on-ground condition */
    private static final double MIN_FALL_DISTANCE = 2.0;

    private static final class PlayerState {
        int falseGroundCount;
        int airTicks;
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

        boolean claimsGround = ctx.onGround;
        double deltaY = ctx.deltaY;
        boolean isFalling = deltaY < -FALLING_VELOCITY_THRESHOLD;
        double fallDistance = ctx.lastY - ctx.y;

        if (!claimsGround) {
            state.airTicks++;
            return;
        }

        if (isFalling && fallDistance > MIN_FALL_DISTANCE) {
            state.falseGroundCount++;
            if (state.falseGroundCount >= MIN_FALSE_GROUND_FLAGS) {
                flag(player);
                resetBuffer(player);
                state.falseGroundCount = 0;
            }
            return;
        }

        if (state.airTicks > MIN_AIR_TIME_FOR_GROUND * TICKS_PER_SECOND) {
            if (Math.abs(deltaY) > 0.5) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                }
            }
        }

        state.airTicks = 0;
        state.falseGroundCount = Math.max(0, state.falseGroundCount - 1);
        decreaseBuffer(player, 0.1);
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
