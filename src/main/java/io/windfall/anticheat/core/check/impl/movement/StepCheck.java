package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * Detects step-height violations — clients that instantly move upward by more than the legitimate
 * maximum step height without jumping.
 *
 * <p>Algorithm: Only activates when the player is on the ground both now and on the previous tick
 * (excludes jumps). The vertical delta (Y increase) is compared against the maximum step height
 * for the player's current state:
 * <ul>
 *   <li>Sneaking: {@value MAX_STEP_HEIGHT_SNEAK} blocks</li>
 *   <li>Climbing (ladder/vine): {@value MAX_STEP_HEIGHT_LADDER} blocks</li>
 *   <li>Normal: version-dependent via {@link VersionPhysics#getStepHeight}</li>
 * </ul>
 *
 * <p>Detection tiers:
 * <ol>
 *   <li>Overshoot &gt; 0.3 blocks → immediate flag (blatant step hack)</li>
 *   <li>Overshoot &gt; {@value STEP_TOLERANCE} blocks → buffer increases by overshoot × 5.0</li>
 *   <li>Buffer exceeds {@value STEP_BUFFER_FLAG_THRESHOLD} → flag</li>
 * </ol>
 *
 * @see VersionPhysics#getStepHeight for protocol-version-dependent step limits
 */
@CheckData(name = "Step A", stableKey = "windfall.movement.step", decay = 0.01, setbackVl = 15, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class StepCheck extends Check implements PacketCheck {

    /** Maximum step height when sneaking — 0.6 blocks (vanilla value) */
    private static final double MAX_STEP_HEIGHT_SNEAK = 0.6;
    /** Maximum step height when on a ladder/vine — 2.0 blocks */
    private static final double MAX_STEP_HEIGHT_LADDER = 2.0;
    /** Minimum Y increase to consider as a step (avoids floating-point noise) and tolerance margin */
    private static final double STEP_TOLERANCE = 0.05;
    /** Buffer threshold at which gradual step violations trigger a flag */
    private static final int STEP_BUFFER_FLAG_THRESHOLD = 3;

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (!(packet instanceof PlayerMoveC2SPacket)) return;
        if (player.isFlying() || player.isGliding() || player.isCachedIsFallFlying()) return;

        if (player.isOnGround() && player.isLastOnGround()) {
            double deltaY = player.getY() - player.getLastY();

            if (deltaY <= 0 || deltaY < STEP_TOLERANCE) return;

            double maxHeight = getMaxStepHeight(player);

            if (deltaY > maxHeight + STEP_TOLERANCE) {
                double overshoot = deltaY - maxHeight;

                if (overshoot > 0.3) {
                    flag(player);
                    return;
                }

                increaseBuffer(player, overshoot * 5.0);
                if (getBuffer(player) > STEP_BUFFER_FLAG_THRESHOLD) {
                    flag(player);
                    resetBuffer(player);
                }
            } else {
                decreaseBuffer(player, 0.1);
            }
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private double getMaxStepHeight(WindfallPlayer player) {
        if (player.isClimbing()) return MAX_STEP_HEIGHT_LADDER;
        if (player.isSneaking()) return MAX_STEP_HEIGHT_SNEAK;
        int protocol = player.getProtocolVersion();
        return VersionPhysics.getStepHeight(protocol);
    }
}
