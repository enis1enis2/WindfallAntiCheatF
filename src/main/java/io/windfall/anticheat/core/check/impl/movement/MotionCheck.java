package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects impossible horizontal and vertical motion values — a general-purpose speed boundary check.
 *
 * <p>Algorithm: Computes the player's actual horizontal speed (Pythagorean of deltaX and deltaZ)
 * and vertical speed (|deltaY|), then compares against hard-coded maximum thresholds based on
 * the player's ground/sprint state:
 * <ul>
 *   <li>On ground, not sprinting: {@value GROUND_MAX_SPEED} blocks/tick (0.28)</li>
 *   <li>On ground, sprinting: {@value SPRINT_GROUND_MAX} blocks/tick (0.36)</li>
 *   <li>Airborne: {@value MAX_PLAYER_SPEED} blocks/tick (1.0)</li>
 *   <li>Vertical (airborne): {@value MAX_VERTICAL_SPEED} blocks/tick (1.5)</li>
 * </ul>
 *
 * <p>Horizontal detection requires {@value MIN_HIGH_SPEED_TICKS} consecutive ticks of exceeding
 * {@code maxHorizontal × 1.2} before buffer starts building — filters out lag spikes and
 * legitimate speed-potion edge cases.
 *
 * <p>This check complements {@link SpeedCheck} which uses full physics prediction. MotionCheck
 * provides a simpler, faster boundary check for blatant violations.
 *
 * @see SpeedCheck for physics-prediction-based speed detection
 * @see FlightCheck for vertical flight detection
 */
@CheckData(name = "Motion A", stableKey = "windfall.movement.motion", decay = 0.01, setbackVl = 20, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class MotionCheck extends Check implements PacketCheck {

    /** Absolute maximum horizontal speed for airborne players (blocks/tick) */
    private static final double MAX_PLAYER_SPEED = 1.0;
    /** Absolute maximum vertical speed (|deltaY|) — catches blatant fly/velocity hacks */
    private static final double MAX_VERTICAL_SPEED = 1.5;
    /** Maximum horizontal speed on ground while walking (blocks/tick) — vanilla: 0.287 */
    private static final double GROUND_MAX_SPEED = 0.28;
    /** Maximum horizontal speed on ground while sprinting (blocks/tick) — vanilla: ~0.373 */
    private static final double SPRINT_GROUND_MAX = 0.36;
    /** Consecutive ticks of high speed before the buffer starts accumulating */
    private static final int MIN_HIGH_SPEED_TICKS = 3;

    private static final class PlayerState {
        int highSpeedTicks;
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
        double deltaX = player.getDeltaX();
        double deltaY = player.getDeltaY();
        double deltaZ = player.getDeltaZ();
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double verticalSpeed = Math.abs(deltaY);

        boolean onGround = player.isOnGround();

        double maxHorizontal = onGround
                ? (player.isSprinting() ? SPRINT_GROUND_MAX : GROUND_MAX_SPEED)
                : MAX_PLAYER_SPEED;

        if (horizontalSpeed > maxHorizontal * 1.2) {
            state.highSpeedTicks++;
            if (state.highSpeedTicks >= MIN_HIGH_SPEED_TICKS) {
                increaseBuffer(player, 1.0);
                if (getBuffer(player) > 3.0) {
                    flag(player);
                    resetBuffer(player);
                    state.highSpeedTicks = 0;
                }
            }
        } else {
            state.highSpeedTicks = Math.max(0, state.highSpeedTicks - 1);
            decreaseBuffer(player, 0.1);
        }

        if (verticalSpeed > MAX_VERTICAL_SPEED && !onGround) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
