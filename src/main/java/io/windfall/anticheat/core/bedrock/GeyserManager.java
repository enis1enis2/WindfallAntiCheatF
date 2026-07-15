package io.windfall.anticheat.core.bedrock;
public class GeyserManager {
    private static GeyserManager instance;
    public static GeyserManager init(Object plugin) { instance = new GeyserManager(); return instance; }
    public boolean isGeyserPresent() { return false; }
    public BedrockInfo getBedrockInfo(java.util.UUID uuid) { return null; }
}
