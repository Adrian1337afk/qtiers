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

/** Fetches and caches profiles from every tier-list site. Never blocks the render thread. */
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
            .followRedirects(HttpClient.Redirect.NORMAL) // mcpvp.com profiles redirect
            .executor(EXECUTOR)
            .build();

    private record Entry(Optional<TierProfile> profile, long expiresAt, boolean loading) {}

    private static final Map<UUID, Map<TierSource, Entry>> CACHE = new ConcurrentHashMap<>();

    private static final java.util.function.BiConsumer<TierSource, Throwable> LOGGER_FAILED =
            (source, e) -> QTiers.LOGGER.warn("Could not load {} profile: {}", source.displayName, e.toString());

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
            fetch(source, source.urlFor(uuid)).whenComplete((result, err) -> {
                boolean failed = err != null;
                Optional<TierProfile> value = failed ? stale : result;
                perSource.put(source, new Entry(value, System.currentTimeMillis() + (failed ? ERROR_TTL_MS : TTL_MS), false));
            });
            return stale;
        }
        return entry.profile();
    }

    /**
     * Starts a lookup on every site for a player name (profile screen), one future per site so each
     * can be shown as soon as it arrives. Each site is retried once; a site that still fails completes
     * exceptionally, so callers can tell "couldn't load" apart from "not ranked" (empty).
     */
    public static Map<TierSource, CompletableFuture<Optional<TierProfile>>> lookupEach(String playerName) {
        String encoded = URLEncoder.encode(playerName, StandardCharsets.UTF_8);
        Map<TierSource, CompletableFuture<Optional<TierProfile>>> futures = new EnumMap<>(TierSource.class);
        for (TierSource source : TierSource.values()) {
            String url = source.urlFor(encoded);
            futures.put(source, fetch(source, url).exceptionallyCompose(e -> fetch(source, url)).whenComplete((r, e) -> {
                if (e != null) LOGGER_FAILED.accept(source, e);
            }));
        }
        return futures;
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

    /** Fetches and parses one site's profile. Empty = player not listed; other failures complete exceptionally. */
    private static CompletableFuture<Optional<TierProfile>> fetch(TierSource source, String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "QTiers-Fabric-Mod/1.0")
                .header("Accept", source == TierSource.MCPVP ? "text/html" : "application/json")
                .GET()
                .build();
        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> source.parser.parse(response.statusCode(), response.uri(), response.body()));
    }
}
