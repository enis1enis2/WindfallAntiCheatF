package io.windfall.anticheat.core.version;

import org.slf4j.Logger;

public class ServerFork {
    private final String displayName;
    private final String forkName;

    private ServerFork(String displayName, String forkName) {
        this.displayName = displayName;
        this.forkName = forkName;
    }

    public static ServerFork detect(Logger logger) {
        String serverBrand = System.getProperty("windfall.fork", "Fabric");
        logger.info("[Windfall] Detected server fork: {}", serverBrand);
        return new ServerFork(serverBrand, serverBrand.toLowerCase());
    }

    public String getDisplayName() { return displayName; }
    public String getForkName() { return forkName; }
    public boolean isFolia() { return false; }
    public boolean isPurpur() { return false; }
}
