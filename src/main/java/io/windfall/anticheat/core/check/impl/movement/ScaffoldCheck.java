package io.windfall.anticheat.core.check.impl.movement;

import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects scaffold (auto-bridge) hacks via three complementary detection vectors:
 * placement speed, tick-based tower detection, and rotation consistency.
 *
 * <p><b>Detection vector 1 — Placement speed:</b>
 * Tracks blocks placed per second in a sliding window and compares against
 * platform-specific thresholds (Java 12.0, Bedrock touch 8.0, controller 9.0, keyboard 10.0).
 * Sprinting players have a lower threshold (4.0 BPS) since sprinting reduces placement precision.
 *
 * <p><b>Detection vector 2 — Tick-based tower detection:</b>
 * Scaffold hacks placing blocks vertically (tower) place one block per game tick. Legitimate
 * tower building requires at least 6 ticks between placements.
 *
 * <p><b>Detection vector 3 — Rotation consistency:</b>
 * Scaffold bots rotate the player's view to face the block being placed, typically snapping
 * to exact yaw/pitch values at inhuman speeds.
 *
 * @see MultiPlaceCheck — companion check for per-tick placement rate
 * @see InvalidPlaceCheck — companion check for occupied-block violations
 */
@CheckData(name = "Scaffold A", stableKey = "windfall.movement.scaffold", decay = 0.005, setbackVl = 30, compat = {CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class ScaffoldCheck extends Check implements PacketCheck {

    private static final double JAVA_MAX_BLOCK_PLACE_PER_SECOND = 12.0;
    private static final double BEDROCK_TOUCH_MAX_BLOCKS_PER_SEC = 8.0;
    private static final double BEDROCK_KB_MAX_BLOCKS_PER_SEC = 10.0;
    private static final double BEDROCK_CONTROLLER_MAX_BLOCKS_PER_SEC = 9.0;
    private static final double SPRINTING_BLOCKS_PER_SEC_THRESHOLD = 4.0;
    private static final long PLACE_WINDOW_MS = 1000;

    private static final int TOWER_TICK_THRESHOLD = 6;
    private static final int TOWER_FAST_PLACEMENT_THRESHOLD = 3;

    private static final float ROTATION_SNAP_THRESHOLD = 45.0f;
    private static final int ROTATION_VIOLATION_THRESHOLD = 3;

    private static final class PlayerState {
        int blocksPlacedThisWindow;
        long windowStartTime;
        double blocksPerSecondAccum;
        int samplesCollected;

        long lastPlacementTick;
        int consecutiveFastPlacements;

        float lastYaw;
        float lastPitch;
        long lastPlacementTimeMs;
        int rotationSnapViolations;
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
        if (packet instanceof PlayerInteractBlockC2SPacket) {
            handleBlockPlace(player);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    public void onTick(WindfallPlayer player, long currentTick) {
        PlayerState state = getState(player);

        if (state.lastPlacementTick > 0 && currentTick - state.lastPlacementTick > TOWER_TICK_THRESHOLD) {
            state.consecutiveFastPlacements = 0;
        }
    }

    private void handleBlockPlace(WindfallPlayer player) {
        PlayerState state = getState(player);
        long now = System.currentTimeMillis();

        checkPlacementSpeed(player, state, now);
        checkTowerDetection(player, state);
        checkRotationConsistency(player, state, now);
    }

    private void checkPlacementSpeed(WindfallPlayer player, PlayerState state, long now) {
        if (state.windowStartTime == 0 || now - state.windowStartTime > PLACE_WINDOW_MS) {
            if (state.blocksPlacedThisWindow > 0) {
                double bps = state.blocksPlacedThisWindow;
                state.blocksPerSecondAccum += bps;
                state.samplesCollected++;
            }
            state.blocksPlacedThisWindow = 0;
            state.windowStartTime = now;
        }

        state.blocksPlacedThisWindow++;
        double bps = state.blocksPlacedThisWindow / Math.max(1.0, (now - state.windowStartTime) / 1000.0);

        io.windfall.anticheat.core.bedrock.GeyserManager geyser = WindfallMod.getInstance().getGeyserManager();
        if (geyser != null && geyser.isBedrockPlayer(player.getUuid())) {
            checkBedrockScaffold(player, bps);
        } else {
            checkJavaScaffold(player, bps);
        }
    }

    private void checkTowerDetection(WindfallPlayer player, PlayerState state) {
        WindfallMod mod = WindfallMod.getInstance();
        if (mod == null || mod.getCheckManager() == null) return;

        long currentTick = mod.getCheckManager().getTickCounter();
        long tickDelta = currentTick - state.lastPlacementTick;

        if (state.lastPlacementTick > 0 && tickDelta <= TOWER_TICK_THRESHOLD) {
            state.consecutiveFastPlacements++;
            if (state.consecutiveFastPlacements >= TOWER_FAST_PLACEMENT_THRESHOLD) {
                increaseBuffer(player, 1.5);
                if (getBuffer(player) > 4.0) {
                    flag(player);
                    resetBuffer(player);
                    state.consecutiveFastPlacements = 0;
                }
            }
        } else {
            state.consecutiveFastPlacements = Math.max(0, state.consecutiveFastPlacements - 1);
        }

        state.lastPlacementTick = currentTick;
    }

    private void checkRotationConsistency(WindfallPlayer player, PlayerState state, long now) {
        float yaw = player.getYaw();
        float pitch = player.getPitch();

        if (state.lastPlacementTimeMs > 0) {
            float deltaYaw = Math.abs(yaw - state.lastYaw);
            if (deltaYaw > 180) deltaYaw = 360 - deltaYaw;
            float deltaPitch = Math.abs(pitch - state.lastPitch);

            if (deltaYaw > ROTATION_SNAP_THRESHOLD || deltaPitch > ROTATION_SNAP_THRESHOLD) {
                state.rotationSnapViolations++;
                if (state.rotationSnapViolations >= ROTATION_VIOLATION_THRESHOLD) {
                    increaseBuffer(player, 1.0);
                    if (getBuffer(player) > 5.0) {
                        flag(player);
                        resetBuffer(player);
                        state.rotationSnapViolations = 0;
                    }
                }
            } else {
                state.rotationSnapViolations = Math.max(0, state.rotationSnapViolations - 1);
            }
        }

        state.lastYaw = yaw;
        state.lastPitch = pitch;
        state.lastPlacementTimeMs = now;
    }

    private void checkJavaScaffold(WindfallPlayer player, double bps) {
        if (bps > JAVA_MAX_BLOCK_PLACE_PER_SECOND) {
            increaseBuffer(player, 1.0);
            if (getBuffer(player) > 5.0) {
                flag(player);
                resetBuffer(player);
            }
        } else if (player.isSprinting() && bps > SPRINTING_BLOCKS_PER_SEC_THRESHOLD) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    private void checkBedrockScaffold(WindfallPlayer player, double bps) {
        io.windfall.anticheat.core.bedrock.GeyserManager geyser = WindfallMod.getInstance().getGeyserManager();
        if (geyser == null) return;
        io.windfall.anticheat.core.bedrock.BedrockInfo info = geyser.getBedrockInfo(player.getUuid());
        if (info == null) return;

        double maxBps;
        if (info.isTouchDevice()) {
            maxBps = BEDROCK_TOUCH_MAX_BLOCKS_PER_SEC;
        } else if (info.isController()) {
            maxBps = BEDROCK_CONTROLLER_MAX_BLOCKS_PER_SEC;
        } else {
            maxBps = BEDROCK_KB_MAX_BLOCKS_PER_SEC;
        }

        if (bps > maxBps) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 8.0) {
                flag(player);
                resetBuffer(player);
            }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }
}
