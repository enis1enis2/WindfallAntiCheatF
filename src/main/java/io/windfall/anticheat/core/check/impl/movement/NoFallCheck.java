package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects no-fall hacks — clients that report {@code onGround=true} while falling, preventing
 * fall damage and enabling fall-speed manipulation.
 *
 * <p>Algorithm: Flags when a player simultaneously:
 * <ol>
 *   <li>Has downward velocity exceeding {@value MIN_FALL_VELOCITY} blocks/tick (actually falling)</li>
 *   <li>Has fallen more than {@value MIN_FALL_DISTANCE} blocks since last position</li>
 *   <li>Claims to be on the ground ({@code onGround=true} in the movement packet)</li>
 * </ol>
 *
 * <p>After {@value MAX_CONSECUTIVE} consecutive violations, the player is flagged. The check also
 * tracks maximum observed fall distance and velocity for alert reporting context.
 *
 * <p>State reset occurs after a flag to avoid over-penalizing a single continuous fall.
 *
 * @see FlightCheck for the complementary no-fall sub-check within flight detection
 * @see GroundSpoofCheck for ground-state verification
 */
@CheckData(name = "NoFall A", stableKey = "windfall.movement.nofall", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class NoFallCheck extends Check implements PacketCheck {

    /** Minimum downward velocity (blocks/tick) to confirm the player is actually falling */
    private static final double MIN_FALL_VELOCITY = 0.3;
    /** Minimum fall distance (blocks) to avoid false positives from minor Y-jitter */
    private static final double MIN_FALL_DISTANCE = 2.0;
    /** Consecutive falling-while-on-ground packets required before flagging */
    private static final int MAX_CONSECUTIVE = 5;

    private static final class PlayerState {
        int consecutiveNoFall;
        double maxFallDistance;
        double maxFallVelocity;
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
        double fallDistance = player.getLastY() - player.getY();

        if (deltaY < -MIN_FALL_VELOCITY && fallDistance > MIN_FALL_DISTANCE && onGround) {
            state.consecutiveNoFall++;

            if (fallDistance > state.maxFallDistance) state.maxFallDistance = fallDistance;
            if (Math.abs(deltaY) > state.maxFallVelocity) state.maxFallVelocity = Math.abs(deltaY);

            if (state.consecutiveNoFall >= MAX_CONSECUTIVE) {
                flag(player);
                state.consecutiveNoFall = 0;
                state.maxFallDistance = 0;
                state.maxFallVelocity = 0;
            }
        } else {
            state.consecutiveNoFall = Math.max(0, state.consecutiveNoFall - 1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
