package io.windfall.anticheat.api;
import io.windfall.anticheat.WindfallMod;
import io.windfall.anticheat.core.player.WindfallPlayer;
import java.util.UUID;
public class WindfallAPIImpl implements WindfallAPI {
    private final WindfallMod mod;
    public WindfallAPIImpl(WindfallMod mod) { this.mod = mod; }
    public WindfallPlayerData getPlayerData(UUID uuid) {
        WindfallPlayer wp = mod.getPlayerManager().get(uuid);
        if (wp == null) return null;
        return new WindfallPlayerData(wp.getUuid(), wp.getName());
    }
    public int getPlayerViolationLevel(UUID uuid, String checkKey) {
        WindfallPlayer wp = mod.getPlayerManager().get(uuid);
        return wp != null ? wp.getViolationLevels().getOrDefault(checkKey, 0) : 0;
    }
    public int getTotalViolationLevel(UUID uuid) {
        WindfallPlayer wp = mod.getPlayerManager().get(uuid);
        return wp != null ? wp.getTotalViolationLevel() : 0;
    }
    public void reloadConfig() { mod.getCheckManager().reloadChecks(); }
    public boolean isCheckEnabled(String checkKey) { return mod.getWindfallConfig().isCheckEnabled(checkKey); }
}
