package io.windfall.anticheat.api;
import java.util.UUID;
public class WindfallPlayerData {
    private final UUID uuid;
    private final String name;
    public WindfallPlayerData(UUID uuid, String name) { this.uuid = uuid; this.name = name; }
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
}
