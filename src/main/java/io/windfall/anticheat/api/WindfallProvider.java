package io.windfall.anticheat.api;
public class WindfallProvider {
    private static WindfallAPI instance;
    public static void register(WindfallAPI api) { instance = api; }
    public static void unregister() { instance = null; }
    public static WindfallAPI get() { return instance; }
}
