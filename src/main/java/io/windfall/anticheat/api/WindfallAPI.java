package io.windfall.anticheat.api;
public interface WindfallAPI {
    WindfallPlayerData getPlayerData(java.util.UUID uuid);
    int getPlayerViolationLevel(java.util.UUID uuid, String checkKey);
    int getTotalViolationLevel(java.util.UUID uuid);
    void reloadConfig();
    boolean isCheckEnabled(String checkKey);
}
