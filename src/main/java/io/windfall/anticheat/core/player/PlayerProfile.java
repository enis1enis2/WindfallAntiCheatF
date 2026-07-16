package io.windfall.anticheat.core.player;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;
    private final String name;
    private final int clientProtocol;
    private final boolean isBedrock;
    private final String bedrockInputMode;
    private final String bedrockDeviceOs;
    private final boolean isViaVersionClient;
    private final int versionGap;

    private int firstJoinUnix;
    private int lastJoinUnix;
    private int totalJoinCount;
    private int punishmentCount;
    private String lastPunishmentReason;
    private int totalViolations;

    public PlayerProfile(UUID uuid, String name, int serverProtocol, int clientProtocol,
                         boolean isBedrock, String bedrockInputMode, String bedrockDeviceOs,
                         boolean isViaVersion) {
        this.uuid = uuid;
        this.name = name;
        this.clientProtocol = clientProtocol;
        this.isBedrock = isBedrock;
        this.bedrockInputMode = bedrockInputMode;
        this.bedrockDeviceOs = bedrockDeviceOs;
        this.isViaVersionClient = isViaVersion;
        this.versionGap = Math.abs(serverProtocol - clientProtocol) / 100;
        this.firstJoinUnix = (int)(System.currentTimeMillis() / 1000);
        this.lastJoinUnix = this.firstJoinUnix;
    }

    public PlayerProfile(UUID uuid, String name) {
        this(uuid, name, 770, 770, false, null, null, false);
    }

    public double getBedrockToleranceMultiplier() {
        if (!isBedrock) return 1.0;
        if ("TOUCH".equals(bedrockInputMode)) return 1.15;
        if ("CONTROLLER".equals(bedrockInputMode)) return 1.10;
        return 1.05;
    }

    public double getVersionGapMultiplier() {
        if (versionGap <= 0) return 1.0;
        return 1.0 + (versionGap * 0.05);
    }

    public double getCombinedToleranceMultiplier() {
        return getBedrockToleranceMultiplier() * getVersionGapMultiplier();
    }

    public boolean isTouchDevice() {
        return "TOUCH".equals(bedrockInputMode);
    }

    public boolean isController() {
        return "CONTROLLER".equals(bedrockInputMode);
    }

    public boolean isBedrockKeyboard() {
        return isBedrock && "KEYBOARD_MOUSE".equals(bedrockInputMode);
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public int getClientProtocol() { return clientProtocol; }
    public boolean isBedrock() { return isBedrock; }
    public String getBedrockInputMode() { return bedrockInputMode; }
    public String getBedrockDeviceOs() { return bedrockDeviceOs; }
    public boolean isViaVersionClient() { return isViaVersionClient; }
    public int getVersionGap() { return versionGap; }
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
