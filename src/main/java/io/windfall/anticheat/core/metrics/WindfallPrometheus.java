package io.windfall.anticheat.core.metrics;

import io.windfall.anticheat.WindfallMod;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class WindfallPrometheus {
    private final WindfallMod mod;
    private final Map<String, AtomicLong> flagCounts = new ConcurrentHashMap<>();
    private final AtomicLong totalFlags = new AtomicLong(0);
    private final AtomicLong activePlayers = new AtomicLong(0);
    private long lastTickTime = System.nanoTime();

    public WindfallPrometheus(WindfallMod mod) { this.mod = mod; }

    public void init(WindfallMod mod) {
        WindfallMod.LOGGER.info("Prometheus metrics initialized");
    }

    public void shutdown() {}

    public void incrementFlags(String checkKey) {
        flagCounts.computeIfAbsent(checkKey, k -> new AtomicLong(0)).incrementAndGet();
        totalFlags.incrementAndGet();
    }

    public void setActivePlayers(long count) { activePlayers.set(count); }

    public void tick() {
        lastTickTime = System.nanoTime();
    }

    public Map<String, Long> getFlagCounts() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        flagCounts.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    public long getTotalFlags() { return totalFlags.get(); }
    public long getActivePlayers() { return activePlayers.get(); }
}
