package io.windfall.anticheat.core.severity;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

public class ViolationPattern {
    private final Path dataDir;
    private final Logger logger;
    private boolean enabled = true;
    private int historyDays = 30;
    private int repeatThreshold = 3;
    private int toggleDetectionWindow = 6000;
    private final Map<UUID, List<ViolationEntry>> violationHistory = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> checkToggleCounts = new ConcurrentHashMap<>();

    static class ViolationEntry implements Serializable {
        final String checkKey;
        final int vl;
        final long tick;
        ViolationEntry(String key, int vl, long tick) {
            this.checkKey = key; this.vl = vl; this.tick = tick;
        }
    }

    public ViolationPattern(Path dataDir, Logger logger) {
        this.dataDir = dataDir;
        this.logger = logger;
    }

    public void loadConfig(boolean enabled, int historyDays, int repeatThreshold, int toggleWindow) {
        this.enabled = enabled;
        this.historyDays = historyDays;
        this.repeatThreshold = repeatThreshold;
        this.toggleDetectionWindow = toggleWindow;
    }

    public void recordViolation(UUID uuid, String checkKey, int vl, long tick) {
        if (!enabled) return;
        violationHistory.computeIfAbsent(uuid, k -> new ArrayList<>()).add(new ViolationEntry(checkKey, vl, tick));
    }

    public boolean isRepeatOffender(UUID uuid) {
        List<ViolationEntry> history = violationHistory.getOrDefault(uuid, Collections.emptyList());
        Map<String, Integer> counts = new HashMap<>();
        for (ViolationEntry e : history) {
            counts.merge(e.checkKey, 1, Integer::sum);
        }
        return counts.values().stream().anyMatch(c -> c >= repeatThreshold);
    }

    public void savePlayerHistory(UUID uuid) {
        List<ViolationEntry> history = violationHistory.get(uuid);
        if (history == null || history.isEmpty()) return;
        try {
            Files.createDirectories(dataDir);
            Path file = dataDir.resolve(uuid.toString() + ".dat");
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
                oos.writeObject(new ArrayList<>(history));
            }
        } catch (IOException e) {
            logger.warn("Failed to save violation history for " + uuid);
        }
    }

    public void pruneOldHistories() {
        long cutoff = System.currentTimeMillis() - ((long) historyDays * 24 * 60 * 60 * 1000);
        for (Map.Entry<UUID, List<ViolationEntry>> entry : violationHistory.entrySet()) {
            entry.getValue().removeIf(e -> e.tick < cutoff);
        }
    }

    public Map<UUID, List<ViolationEntry>> getViolationHistory() { return violationHistory; }
}
