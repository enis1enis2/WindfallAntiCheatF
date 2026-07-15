package io.windfall.anticheat.core.fingerprint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PacketFingerprint {
    private boolean enabled = true;
    private int minSeverityToFlag = 60;
    private int maxAgeTicks = 6000;
    private final Map<UUID, List<PacketEntry>> fingerprints = new ConcurrentHashMap<>();

    static class PacketEntry {
        final long timestamp;
        final int packetType;
        final int size;
        PacketEntry(long ts, int type, int size) {
            this.timestamp = ts; this.packetType = type; this.size = size;
        }
    }

    public void loadConfig(boolean enabled, int minSeverity, int maxAge) {
        this.enabled = enabled;
        this.minSeverityToFlag = minSeverity;
        this.maxAgeTicks = maxAge;
    }

    public void recordPacketInterval(UUID uuid, int intervalMs) {
        if (!enabled) return;
        fingerprints.computeIfAbsent(uuid, k -> new ArrayList<>()).add(
            new PacketEntry(System.currentTimeMillis(), 0, intervalMs));
    }

    public int getFingerprintSeverity(UUID uuid) {
        List<PacketEntry> entries = fingerprints.getOrDefault(uuid, Collections.emptyList());
        if (entries.size() < 10) return 0;
        int suspicious = 0;
        for (int i = 1; i < entries.size(); i++) {
            long interval = entries.get(i).timestamp - entries.get(i - 1).timestamp;
            if (interval < 5) suspicious++;
        }
        return Math.min(100, (suspicious * 100) / entries.size());
    }

    public void removePlayer(UUID uuid) { fingerprints.remove(uuid); }

    public void onTick(long tickCounter) {
        long cutoff = System.currentTimeMillis() - (maxAgeTicks * 50L);
        for (Map.Entry<UUID, List<PacketEntry>> entry : fingerprints.entrySet()) {
            entry.getValue().removeIf(e -> e.timestamp < cutoff);
        }
    }
}
