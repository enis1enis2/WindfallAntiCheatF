package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.check.PacketCheck;
import io.windfall.anticheat.core.physics.VersionPhysics;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@CheckData(name = "Reach A", stableKey = "windfall.combat.reach", decay = 0.05, setbackVl = 10,
    compat = {CompatFlag.VIAVERSION_SENSITIVE, CompatFlag.RELAX_ON_MISMATCH}, relaxMultiplier = 1.3)
public class ReachCheck extends Check implements PacketCheck {

    private static final double TOLERANCE = 0.1;
    private static final double PROTOCOL_MARGIN_LEGACY = 0.10;
    private static final double PROTOCOL_MARGIN_MODERN = 0.03;
    private static final double PLAYER_WIDTH = 0.6;
    private static final double PLAYER_HEIGHT = 1.8;
    private static final double ENTITY_DEFAULT_SIZE = 0.25;
    private static final int ROLLING_WINDOW = 20;
    private static final int ATTACKER_SNAPSHOT_WINDOW = 5;
    private static final long ATTACKER_SNAPSHOT_MAX_AGE_MS = 250L;

    private static final ConcurrentHashMap<Integer, TrackedEntity> trackedEntities = new ConcurrentHashMap<>();

    private static final class PlayerState {
        final ArrayDeque<Double> reachSamples = new ArrayDeque<>();
        final ArrayDeque<AttackerSnapshot> attackerSnapshots = new ArrayDeque<>();
    }

    private static final class AttackerSnapshot {
        final double eyeX, eyeY, eyeZ;
        final long timestamp;

        AttackerSnapshot(double eyeX, double eyeY, double eyeZ, long timestamp) {
            this.eyeX = eyeX;
            this.eyeY = eyeY;
            this.eyeZ = eyeZ;
            this.timestamp = timestamp;
        }
    }

    private final ConcurrentHashMap<UUID, PlayerState> stateMap = new ConcurrentHashMap<>();

    private PlayerState getState(UUID uuid) {
        return stateMap.computeIfAbsent(uuid, k -> new PlayerState());
    }

    @Override
    public void removePlayer(UUID uuid) {
        stateMap.remove(uuid);
    }

    public static void trackSpawn(int entityId, double x, double y, double z) {
        trackedEntities.put(entityId, new TrackedEntity(x, y, z, System.currentTimeMillis()));
    }

    public static void trackMove(int entityId, double x, double y, double z) {
        trackedEntities.merge(entityId,
                new TrackedEntity(x, y, z, System.currentTimeMillis()),
                (old, fresh) -> new TrackedEntity(fresh.x, fresh.y, fresh.z, fresh.timestamp));
    }

    public static void trackRemove(int entityId) {
        trackedEntities.remove(entityId);
    }

    public static void cleanup(long maxAgeMs) {
        long now = System.currentTimeMillis();
        trackedEntities.entrySet().removeIf(e -> now - e.getValue().timestamp > maxAgeMs);
    }

    @Override
    public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket) {
            recordAttackerSnapshot(player);
        }

        if (!(packet instanceof net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket p)) return;

        final boolean[] isAttack = {false};
        final net.minecraft.entity.Entity[] targetEntity = {null};
        p.handle(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler() {
            public void interact(net.minecraft.util.Hand hand) {}
            public void attack() {
                isAttack[0] = true;
                try {
                    targetEntity[0] = p.getEntity(player.getServerPlayer().getServerWorld());
                } catch (Exception ignored) {}
            }
            public void interactAt(net.minecraft.util.Hand hand, net.minecraft.util.math.Vec3d location) {}
        });
        if (!isAttack[0] || targetEntity[0] == null) return;

        PlayerState state = getState(player.getUuid());
        int ping = player.getTransactionPing();

        double targetHalfWidth = targetEntity[0].getWidth() / 2.0;
        double targetHeight = targetEntity[0].getHeight();
        double targetMinX = targetEntity[0].getX() - targetHalfWidth;
        double targetMinY = targetEntity[0].getY();
        double targetMinZ = targetEntity[0].getZ() - targetHalfWidth;
        double targetMaxX = targetEntity[0].getX() + targetHalfWidth;
        double targetMaxY = targetEntity[0].getY() + targetHeight;
        double targetMaxZ = targetEntity[0].getZ() + targetHalfWidth;

        double bestReach = Double.MAX_VALUE;
        long now = System.currentTimeMillis();

        ArrayDeque<AttackerSnapshot> snapshots = state.attackerSnapshots;

        if (snapshots.isEmpty()) {
            bestReach = computeAABBDistance(
                    player.getX(), player.getY() + getEyeHeight(player), player.getZ(),
                    targetMinX, targetMinY, targetMinZ, targetMaxX, targetMaxY, targetMaxZ);
        } else {
            for (AttackerSnapshot snap : snapshots) {
                if (now - snap.timestamp > ATTACKER_SNAPSHOT_MAX_AGE_MS) continue;
                double reach = computeAABBDistance(snap.eyeX, snap.eyeY, snap.eyeZ,
                        targetMinX, targetMinY, targetMinZ, targetMaxX, targetMaxY, targetMaxZ);
                if (reach < bestReach) bestReach = reach;
            }
            double currentReach = computeAABBDistance(
                    player.getX(), player.getY() + getEyeHeight(player), player.getZ(),
                    targetMinX, targetMinY, targetMinZ, targetMaxX, targetMaxY, targetMaxZ);
            if (currentReach < bestReach) bestReach = currentReach;
        }

        state.reachSamples.addLast(bestReach);
        if (state.reachSamples.size() > ROLLING_WINDOW) state.reachSamples.removeFirst();

        double limit = getReachLimit(player);
        double pingTolerance = Math.min(ping * 0.001, 0.3);
        double protocolMargin = player.getProtocolVersion() < 107 ? PROTOCOL_MARGIN_LEGACY : PROTOCOL_MARGIN_MODERN;
        double effectiveLimit = limit + TOLERANCE + pingTolerance + protocolMargin;

        if (bestReach > effectiveLimit) {
            flag(player);
            return;
        }

        double avgReach = state.reachSamples.stream()
                .mapToDouble(Double::doubleValue).average().orElse(0.0);

        if (avgReach > limit + 0.05 && state.reachSamples.size() >= ROLLING_WINDOW / 2) {
            increaseBuffer(player, 0.5);
            if (getBuffer(player) > 3.0) {
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

    private void recordAttackerSnapshot(WindfallPlayer player) {
        PlayerState state = getState(player.getUuid());
        state.attackerSnapshots.addLast(new AttackerSnapshot(
                player.getX(), player.getY() + getEyeHeight(player), player.getZ(),
                System.currentTimeMillis()));
        while (state.attackerSnapshots.size() > ATTACKER_SNAPSHOT_WINDOW) state.attackerSnapshots.removeFirst();
    }

    private double computeAABBDistance(double eyeX, double eyeY, double eyeZ,
                                       double bbMinX, double bbMinY, double bbMinZ,
                                       double bbMaxX, double bbMaxY, double bbMaxZ) {
        double closestX = Math.max(bbMinX, Math.min(eyeX, bbMaxX));
        double closestY = Math.max(bbMinY, Math.min(eyeY, bbMaxY));
        double closestZ = Math.max(bbMinZ, Math.min(eyeZ, bbMaxZ));
        double dx = eyeX - closestX;
        double dy = eyeY - closestY;
        double dz = eyeZ - closestZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private double getReachLimit(WindfallPlayer player) {
        int protocol = player.getProtocolVersion();
        double baseReach = VersionPhysics.getMaxReach(protocol);

        if (VersionPhysics.hasAttackCooldown(protocol)) {
            int cooldown = player.getAttackCooldown();
            double cooldownModifier = Math.min(cooldown / 20.0, 1.0);
            return baseReach + (VersionPhysics.getCooldownReachBonus(protocol) * cooldownModifier);
        }

        return baseReach;
    }

    private double getEyeHeight(WindfallPlayer player) {
        int protocol = player.getProtocolVersion();
        return VersionPhysics.getPlayerEyeHeight(player.isSneaking(), protocol);
    }

    private static final class TrackedEntity {
        final double x, y, z;
        final long timestamp;

        TrackedEntity(double x, double y, double z, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.timestamp = timestamp;
        }
    }
}
