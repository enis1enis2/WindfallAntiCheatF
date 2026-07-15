package io.windfall.anticheat.core.player;

import java.util.UUID;

public class PlayerProfile {
    private final UUID uuid;
    private final String name;
    private int firstJoinUnix;
    private int lastJoinUnix;
    private int totalJoinCount;
    private int punishmentCount;
    private String lastPunishmentReason;
    private int totalViolations;

    public PlayerProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoinUnix = (int)(System.currentTimeMillis() / 1000);
        this.lastJoinUnix = this.firstJoinUnix;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public int getFirstJoinUnix() { return firstJoinUnix; }
    public int getLastJoinUnix() { return lastJoinUnix; }
    public void setLastJoinUnix(int t) { this.lastJoinUnix = t; }
    public int getTotalJoinCount() { return totalJoinCount; }
    public void incrementJoinCount() { this.totalJoinCount++; }
    public int getPunishmentCount() { return punishmentCount; }
    public void incrementPunishmentCount() { this.punishmentCount++; }
    public String getLastPunishmentReason() { return lastPunishmentReason; }
    public void setLastPunishmentReason(String r) { this.lastPunishmentReason = r; }
    public int getTotalViolations() { return totalViolations; }
    public void addViolations(int v) { this.totalViolations += v; }
}
