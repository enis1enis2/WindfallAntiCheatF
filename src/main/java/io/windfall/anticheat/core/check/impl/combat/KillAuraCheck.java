package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.check.PacketCheck;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Kill Aura A", stableKey = "windfall.combat.killaura", decay = 0.005, setbackVl = 25,
    compat = {CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.2)
public class KillAuraCheck extends Check implements PacketCheck {

    private static final int MAX_TARGETS_PER_SECOND_LEGACY = 6;
    private static final int MAX_TARGETS_PER_SECOND_MODERN = 4;
    private static final int SWING_WINDOW_MS = 1000;
    private static final double SNAP_TO_TARGET_THRESHOLD = 60.0;
    private static final int MIN_SNAP_COUNT = 3;
    private static final double BOT_ROTATION_SYMMETRY_THRESHOLD = 0.95;
    private static final double LEGACY_STRAFE_THRESHOLD = 0.15;
    private static final int LEGACY_BUFFER_MULTIPLIER = 2;

    private static final class PlayerState {
        final ArrayDeque<TargetEvent> recentTargets = new ArrayDeque<>();
        final ArrayDeque<Float> recentYawDeltas = new ArrayDeque<>();
        float lastYaw;
        boolean hasLastYaw;
        int snapCount;
        int totalAttacks;
        boolean legacyClient;
        boolean strafing;
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        PlayerState state = getState(player.getUuid());

        if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p) {
            final boolean[] isAttack = {false};
            p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
                public void interact(net.minecraft.util.Hand hand) {}
                public void attack() { isAttack[0] = true; }
                public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {}
            });
            if (isAttack[0]) {
                handleAttack(player, p, state);
            }
        } else if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket) {
            handleRotation(player, state);
        }
    }

    @Override
    public void onPacketSend(WindfallPlayer player, Object packet) {
    }

    private void handleAttack(WindfallPlayer player, net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p, PlayerState state) {
        int protocol = player.getProtocolVersion();
        state.legacyClient = protocol < 107;

        int targetId;
        try {
            net.minecraft.entity.Entity target = p.getEntity(player.getServerPlayer().getServerWorld());
            targetId = target != null ? target.getId() : -1;
        } catch (Exception e) {
            targetId = -1;
        }

        long now = System.currentTimeMillis();
        state.recentTargets.addLast(new TargetEvent(targetId, now));
        while (!state.recentTargets.isEmpty() && now - state.recentTargets.peekFirst().timestamp > SWING_WINDOW_MS) {
            state.recentTargets.removeFirst();
        }

        state.totalAttacks++;

        if (state.totalAttacks > 0 && state.totalAttacks % 20 == 0) {
            checkMultiAura(player, state);
        }

        checkRotationSymmetry(player, state);
    }

    private void handleRotation(WindfallPlayer player, PlayerState state) {
        float yaw = player.getYaw();

        if (state.legacyClient) {
            double lateralSpeed = player.getHorizontalSpeed();
            state.strafing = lateralSpeed > LEGACY_STRAFE_THRESHOLD;
        }

        if (!state.hasLastYaw) {
            state.lastYaw = yaw;
            state.hasLastYaw = true;
            return;
        }

        float deltaYaw = yaw - state.lastYaw;
        if (deltaYaw > 180) deltaYaw -= 360;
        if (deltaYaw < -180) deltaYaw += 360;

        state.recentYawDeltas.addLast(deltaYaw);
        if (state.recentYawDeltas.size() > 20) {
            state.recentYawDeltas.removeFirst();
        }

        if (Math.abs(deltaYaw) > SNAP_TO_TARGET_THRESHOLD) {
            state.snapCount++;
        }

        state.lastYaw = yaw;
    }

    private void checkMultiAura(WindfallPlayer player, PlayerState state) {
        int uniqueTargets = (int) state.recentTargets.stream()
                .map(te -> te.targetId)
                .distinct()
                .count();

        int protocol = player.getProtocolVersion();
        int maxTargets = protocol < 107 ? MAX_TARGETS_PER_SECOND_LEGACY : MAX_TARGETS_PER_SECOND_MODERN;

        io.windfall.anticheat.core.bedrock.GeyserManager geyser = io.windfall.anticheat.WindfallMod.getInstance().getGeyserManager();
        if (geyser != null && geyser.isBedrockPlayer(player.getUuid())) {
            maxTargets += 2;
        }

        if (uniqueTargets > maxTargets) {
            double bufferIncrease = state.legacyClient ? 1.0 : 2.0;
            if (state.legacyClient && state.strafing) {
                bufferIncrease *= 0.5;
            }
            increaseBuffer(player, bufferIncrease);
            double flagThreshold = state.legacyClient ? 7.0 : 5.0;
            if (getBuffer(player) > flagThreshold) {
                flag(player);
                resetBuffer(player);
            }
        }
    }

    private void checkRotationSymmetry(WindfallPlayer player, PlayerState state) {
        if (state.snapCount < MIN_SNAP_COUNT) return;
        if (state.recentYawDeltas.size() < 10) return;

        long positiveCount = state.recentYawDeltas.stream().filter(d -> d > 0.5).count();
        long negativeCount = state.recentYawDeltas.stream().filter(d -> d < -0.5).count();
        long total = positiveCount + negativeCount;

        if (total < 10) return;

        double symmetryRatio = Math.min(positiveCount, negativeCount) / (double) total;

        double threshold = BOT_ROTATION_SYMMETRY_THRESHOLD;
        io.windfall.anticheat.core.bedrock.GeyserManager geyser2 = io.windfall.anticheat.WindfallMod.getInstance().getGeyserManager();
        if (geyser2 != null && geyser2.isBedrockPlayer(player.getUuid())) {
            threshold = 0.98;
        }

        if (symmetryRatio > threshold) {
            double bufferIncrease = state.legacyClient ? 0.8 : 1.5;
            if (state.legacyClient && state.strafing) {
                bufferIncrease *= 0.5;
            }
            increaseBuffer(player, bufferIncrease);
            double flagThreshold = state.legacyClient ? 6.0 : 4.0;
            if (getBuffer(player) > flagThreshold) {
                flag(player);
                resetBuffer(player);
                state.snapCount = 0;
            }
        } else {
            decreaseBuffer(player, 0.2);
        }

        if (state.snapCount > MIN_SNAP_COUNT * 3) {
            state.snapCount = 0;
        }
    }

    private static final class TargetEvent {
        final int targetId;
        final long timestamp;

        TargetEvent(int targetId, long timestamp) {
            this.targetId = targetId;
            this.timestamp = timestamp;
        }
    }
}
