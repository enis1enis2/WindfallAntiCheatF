package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects players who bypass the vanilla slowdown applied when using items
 * (bow, shield, food, etc.). In survival Minecraft, using an item reduces
 * movement speed to ~20% of normal; hacked clients often ignore this penalty.
 *
 * <p><b>Algorithm:</b> When a player is flagged as using an item, each movement
 * packet is checked against the maximum expected speed ({@value BASE_WALK_SPEED} *
 * {@value SPRINT_MULTIPLIER} * 0.9). If the actual horizontal speed exceeds
 * 90% of that ceiling, the buffer increases by 0.8 per tick. Once the buffer
 * surpasses 4.0 the player is flagged. The buffer decays by 0.1 each tick when
 * no violation is detected.</p>
 *
 * @see CompatFlag#RELAX_ON_MISMATCH
 * @see Check
 * @see PacketCheck
 */
@CheckData(name = "NoSlow A", stableKey = "windfall.movement.noslow", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class NoSlowCheck extends Check implements PacketCheck {

    /** Base horizontal walk speed in blocks/tick for a non-sprinting player (~4.317 m/s). */
    private static final double BASE_WALK_SPEED = 0.102;

    /** Sprint speed multiplier applied on top of base walk speed. */
    private static final double SPRINT_MULTIPLIER = 1.3;

    /** Minimum horizontal speed (blocks/tick) required before the check activates. */
    private static final double MIN_SPEED_FOR_CHECK = 0.05;

    /** Number of ticks an item must be in use before the slowdown check applies. */
    private static final int MIN_USING_ITEM_TICKS = 3;

    private static final class PlayerState {
        boolean usingItem;
        int usingItemTicks;
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
        if (packet instanceof PlayerInteractItemC2SPacket) {
            PlayerState state = getState(player);
            state.usingItem = true;
            state.usingItemTicks = 0;
            return;
        }

        if (!(packet instanceof PlayerMoveC2SPacket)) return;

        PlayerState state = getState(player);

        if (state.usingItem) {
            state.usingItemTicks++;
            if (state.usingItemTicks > 20) {
                state.usingItem = false;
                state.usingItemTicks = 0;
            }
        }

        double deltaX = player.getDeltaX();
        double deltaZ = player.getDeltaZ();
        double horizontalSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (horizontalSpeed < MIN_SPEED_FOR_CHECK) {
            decreaseBuffer(player, 0.1);
            return;
        }

        double maxExpectedSpeed = BASE_WALK_SPEED * SPRINT_MULTIPLIER;

        if (state.usingItem && horizontalSpeed > maxExpectedSpeed * 0.9) {
            increaseBuffer(player, 0.8);
            if (getBuffer(player) > 4.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }
}
