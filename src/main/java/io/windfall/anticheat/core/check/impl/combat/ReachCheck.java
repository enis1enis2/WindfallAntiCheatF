package io.windfall.anticheat.core.check.impl.combat;

import io.windfall.anticheat.core.check.*;
import io.windfall.anticheat.core.player.WindfallPlayer;
import io.windfall.anticheat.core.util.MathUtil;
import java.util.*;

@CheckData(name="Reach A", stableKey="windfall.combat.reach", decay=0.01, setbackVl=15, compat={CompatFlag.LAG_COMPENSATED})
public class ReachCheck extends Check implements PacketCheck {
    private static final double MAX_REACH = 3.0;
    private static final long ATTACKER_SNAPSHOT_MAX_AGE_MS = 250;
    private static final int MAX_SNAPSHOTS = 5;
    private static final Map<UUID, java.util.Deque<AttackerSnapshot>> attackerSnapshots = new HashMap<>();

    static class AttackerSnapshot {
        final double eyeX, eyeY, eyeZ;
        final long timestamp;
        AttackerSnapshot(double x, double y, double z) {
            this.eyeX = x; this.eyeY = y; this.eyeZ = z;
            this.timestamp = System.currentTimeMillis();
        }
    }

    @Override public void onPacketReceive(WindfallPlayer player, Object packet) {
        if (packet instanceof net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket) {
            net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket p = (net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket) packet;
            recordAttackerSnapshot(player);
        }
        if (!(packet instanceof net.minecraft.network.packet.c2s.play.InteractEntityC2SPacket)) return;
        net.minecraft.network.packet.c2s.play.InteractEntityC2SPacket p = (net.minecraft.network.packet.c2s.play.InteractEntityC2SPacket) packet;
        if (p.getType() != net.minecraft.network.packet.c2s.play.InteractEntityC2SPacket.Type.ATTACK) return;

        java.util.Deque<AttackerSnapshot> snapshots = attackerSnapshots.getOrDefault(player.getUuid(), new ArrayDeque<>());
        long now = System.currentTimeMillis();
        double minReach = Double.MAX_VALUE;

        for (AttackerSnapshot snap : snapshots) {
            if (now - snap.timestamp > ATTACKER_SNAPSHOT_MAX_AGE_MS) continue;
            double dx = player.getX() - snap.eyeX;
            double dy = (player.getY() + player.getEyeHeight() / 2.0) - snap.eyeY;
            double dz = player.getZ() - snap.eyeZ;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            minReach = Math.min(minReach, dist);
        }

        double dx = player.getX() - player.getLastX();
        double dy = (player.getY() + player.getEyeHeight() / 2.0) - player.getY();
        double dz = player.getZ() - player.getLastZ();
        double currentDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        minReach = Math.min(minReach, currentDist);

        if (minReach > MAX_REACH) {
            increaseBuffer(player, (minReach - MAX_REACH) / MAX_REACH);
            if (getBuffer(player) > 2.0) { flag(player); resetBuffer(player); }
        } else {
            decreaseBuffer(player, 0.1);
        }
    }

    private void recordAttackerSnapshot(WindfallPlayer player) {
        java.util.Deque<AttackerSnapshot> snapshots = attackerSnapshots.computeIfAbsent(player.getUuid(), k -> new ArrayDeque<>());
        snapshots.offer(new AttackerSnapshot(player.getX(), player.getY() + player.getEyeHeight(), player.getZ()));
        while (snapshots.size() > MAX_SNAPSHOTS) snapshots.pollFirst();
    }

    @Override public void onPacketSend(WindfallPlayer player, Object packet) {}

    public static void cleanup(long maxAgeMs) {
        long now = System.currentTimeMillis();
        attackerSnapshots.values().forEach(q -> q.removeIf(s -> now - s.timestamp > maxAgeMs));
    }

    @Override public void removePlayer(UUID uuid) { attackerSnapshots.remove(uuid); }
}
