package dev.qtiers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Fetches and caches profiles from all three sites. Never blocks the render thread. */
public final class TierApi {
    private static final long TTL_MS = Duration.ofMinutes(10).toMillis();
    private static final long ERROR_TTL_MS = Duration.ofMinutes(1).toMillis();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "QTiers-Fetcher");
        t.setDaemon(true);
        return t;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .executor(EXECUTOR)
            .build();

    private record Entry(Optional<TierProfile> profile, long expiresAt, boolean loading) {}

    private static final Map<UUID, Map<TierSource, Entry>> CACHE = new ConcurrentHashMap<>();

    private TierApi() {}

    /**
     * Returns the cached profile if present, kicking off a background fetch when missing or stale.
     * Empty means "not loaded yet" or "not on this site".
     */
    public static Optional<TierProfile> getCached(UUID uuid, TierSource source) {
        Map<TierSource, Entry> perSource = CACHE.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
        Entry entry = perSource.get(source);
        long now = System.currentTimeMillis();
        if (entry == null || (!entry.loading() && now > entry.expiresAt())) {
            Optional<TierProfile> stale = entry == null ? Optional.empty() : entry.profile();
            perSource.put(source, new Entry(stale, Long.MAX_VALUE, true));
            fetch(source.urlFor(uuid)).whenComplete((result, err) -> {
                boolean failed = err != null;
                Optional<TierProfile> value = failed ? stale : result;
                perSource.put(source, new Entry(value, System.currentTimeMillis() + (failed ? ERROR_TTL_MS : TTL_MS), false));
            });
            return stale;
        }
        return entry.profile();
    }

    /** Fetches every site for a player name (used by the /tiers command). */
    public static CompletableFuture<Map<TierSource, Optional<TierProfile>>> lookupAll(String playerName) {
        String encoded = URLEncoder.encode(playerName, StandardCharsets.UTF_8);
        Map<TierSource, CompletableFuture<Optional<TierProfile>>> futures = new EnumMap<>(TierSource.class);
        for (TierSource source : TierSource.values()) {
            futures.put(source, fetch(source.urlFor(encoded)).exceptionally(e -> Optional.empty()));
        }
        return CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new)).thenApply(v -> {
            Map<TierSource, Optional<TierProfile>> out = new EnumMap<>(TierSource.class);
            futures.forEach((s, f) -> out.put(s, f.join()));
            return out;
        });
    }

    public static void clearCache() {
        CACHE.clear();
    }

    /** Resolves a player's name to [uuid, correctly-cased name] via Mojang; empty if unknown. */
    public static CompletableFuture<Optional<String[]>> resolvePlayer(String playerName) {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + URLEncoder.encode(playerName, StandardCharsets.UTF_8);
        return HTTP.sendAsync(request(url), HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) return Optional.<String[]>empty();
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return Optional.of(new String[]{json.get("id").getAsString(), json.get("name").getAsString()});
        }).exceptionally(e -> Optional.empty());
    }

    /** Downloads raw bytes (used for skin renders). Fails on any non-200 response. */
    public static CompletableFuture<byte[]> fetchBytes(String url) {
        return HTTP.sendAsync(request(url), HttpResponse.BodyHandlers.ofByteArray()).thenApply(response -> {
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
            return response.body();
        });
    }

    private static HttpRequest request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "QTiers-Fabric-Mod/1.0")
                .GET()
                .build();
    }

    /** 404 → Optional.empty() (player not listed). Other failures complete exceptionally. */
    private static CompletableFuture<Optional<TierProfile>> fetch(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "QTiers-Fabric-Mod/1.0")
                .header("Accept", "application/json")
                .GET()
                .build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() == 404) return Optional.<TierProfile>empty();
            if (response.statusCode() != 200) {
                throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("error")) return Optional.<TierProfile>empty();
            return Optional.of(TierProfile.parse(json));
        });
    }
}
