package io.windfall.anticheat.core.alert;

import com.google.gson.JsonObject;
import io.windfall.anticheat.WindfallMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordWebhook {
    private final WindfallMod mod;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Map<String, Long> rateLimitMap = new ConcurrentHashMap<>();

    public DiscordWebhook(WindfallMod mod) { this.mod = mod; }

    public void send(String playerName, String checkName, int vl, String extra) {
        String url = mod.getWindfallConfig().getDiscordWebhookUrl();
        if (url == null || url.isEmpty()) return;

        long now = System.currentTimeMillis();
        Long last = rateLimitMap.get(playerName);
        if (last != null && (now - last) < mod.getWindfallConfig().getDiscordRateLimitMs()) return;
        rateLimitMap.put(playerName, now);

        try {
            JsonObject embed = new JsonObject();
            embed.addProperty("title", "Windfall Alert");
            embed.addProperty("color", mod.getWindfallConfig().getDiscordEmbedColor(vl));
            JsonObject fields = new JsonObject();
            fields.addProperty("name", "Player");
            fields.addProperty("value", playerName);
            fields.addProperty("inline", true);
            embed.add("fields", fields);

            JsonObject body = new JsonObject();
            if (mod.getWindfallConfig().isDiscordMentionOnHighVl() && vl >= mod.getWindfallConfig().getDiscordMentionThreshold()) {
                body.addProperty("content", "@here");
            }
            body.add("embeds", new com.google.gson.JsonArray());
            body.getAsJsonArray("embeds").add(embed);

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
            client.sendAsync(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            WindfallMod.LOGGER.debug("Discord webhook error: {}", e.getMessage());
        }
    }

    public void cleanupStaleEntries() {
        long cutoff = System.currentTimeMillis() - 300000;
        rateLimitMap.entrySet().removeIf(e -> e.getValue() < cutoff);
    }
}
