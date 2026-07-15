package io.windfall.anticheat.core.bedrock;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class GeysersTracker {
    private final Map<UUID, BedrockInfo> bedrockPlayers = new ConcurrentHashMap<>();
    public BedrockInfo getBedrockInfo(UUID uuid) { return bedrockPlayers.get(uuid); }
}
