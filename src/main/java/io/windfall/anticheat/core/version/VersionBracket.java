package io.windfall.anticheat.core.version;

public enum VersionBracket {
    LEGACY("1.7-1.8", 4, 47),
    OLD("1.9-1.12", 107, 340),
    MID("1.13-1.15", 393, 573),
    NEW("1.16-1.19", 736, 759),
    MODERN("1.20+", 760, 99999);

    private final String label;
    private final int minProtocol;
    private final int maxProtocol;

    VersionBracket(String label, int minProtocol, int maxProtocol) {
        this.label = label;
        this.minProtocol = minProtocol;
        this.maxProtocol = maxProtocol;
    }

    public static VersionBracket fromProtocol(int protocol) {
        for (VersionBracket b : values()) {
            if (protocol >= b.minProtocol && protocol <= b.maxProtocol) return b;
        }
        return MODERN;
    }

    public String getLabel() { return label; }
    public int getMinProtocol() { return minProtocol; }
    public int getMaxProtocol() { return maxProtocol; }
}
