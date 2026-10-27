package dev.qtiers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Fetches and caches profiles from every tier-list site. Never blocks the render thread.
 * The cache is also saved to .minecraft/qtiers/cache.json, so tiers show the moment you join
 * and are refreshed in the background once they are older than {@link #TTL_MS}.
 */
public final class TierApi {
    private static final long TTL_MS = Duration.ofMinutes(10).toMillis();
    private static final long ERROR_TTL_MS = Duration.ofMinutes(1).toMillis();
    /** Saved results older than this are dropped instead of being shown. */
    private static final long DISK_MAX_AGE_MS = Duration.ofDays(7).toMillis();
    private static final int DISK_MAX_PLAYERS = 5000;
    private static final Path DISK_FILE = FabricLoader.getInstance().getGameDir().resolve("qtiers").resolve("cache.json");
    private static final Gson GSON = new Gson();

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

    /** {@code fetchedAt} is when {@code profile} was fetched; 0 if it never was. */
    private record Entry(Optional<TierProfile> profile, long expiresAt, boolean loading, long fetchedAt) {}

    /** One saved site result; a null profile means "not ranked on this site". */
    private record SavedEntry(TierProfile profile, long fetchedAt) {}

    private static final Map<UUID, Map<TierSource, Entry>> CACHE = new ConcurrentHashMap<>();
    private static final AtomicBoolean DIRTY = new AtomicBoolean();
    private static final ScheduledExecutorService SAVER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "QTiers-Cache");
        t.setDaemon(true);
        return t;
    });

    private static final java.util.function.BiConsumer<TierSource, Throwable> LOGGER_FAILED =
            (source, e) -> QTiersLog.LOGGER.warn("Could not load {} profile: {}", source.displayName, e.toString());

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
            long staleFetchedAt = entry == null ? 0 : entry.fetchedAt();
            perSource.put(source, new Entry(stale, Long.MAX_VALUE, true, staleFetchedAt));
            fetch(source, source.urlFor(uuid)).whenComplete((result, err) -> {
                long done = System.currentTimeMillis();
                if (err != null) {
                    perSource.put(source, new Entry(stale, done + ERROR_TTL_MS, false, staleFetchedAt));
                } else {
                    perSource.put(source, new Entry(result, done + TTL_MS, false, done));
                    DIRTY.set(true);
                }
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

    /** Refetches every player's tiers. The old tiers keep showing until the new ones arrive. */
    public static void clearCache() {
        for (Map<TierSource, Entry> perSource : CACHE.values()) {
            perSource.replaceAll((source, e) -> e.loading() ? e : new Entry(e.profile(), 0, false, e.fetchedAt()));
        }
    }

    // ---------- disk cache ----------

    /** Loads saved tiers in the background and saves changes every 30 seconds. Call once at startup. */
    public static void startDiskCache() {
        SAVER.execute(TierApi::loadFromDisk);
        SAVER.scheduleWithFixedDelay(() -> {
            if (DIRTY.getAndSet(false)) saveToDisk();
        }, 30, 30, TimeUnit.SECONDS);
    }

    /** Saves right away if anything changed (game closing). */
    public static void flushDiskCache() {
        if (DIRTY.getAndSet(false)) saveToDisk();
    }

    private static void loadFromDisk() {
        if (!Files.exists(DISK_FILE)) return;
        try {
            Map<UUID, Map<TierSource, SavedEntry>> saved = GSON.fromJson(Files.readString(DISK_FILE),
                    new TypeToken<Map<UUID, Map<TierSource, SavedEntry>>>() {}.getType());
            if (saved == null) return;
            long now = System.currentTimeMillis();
            int loaded = 0;
            for (Map.Entry<UUID, Map<TierSource, SavedEntry>> player : saved.entrySet()) {
                if (player.getKey() == null || player.getValue() == null) continue;
                Map<TierSource, Entry> perSource = CACHE.computeIfAbsent(player.getKey(), u -> new ConcurrentHashMap<>());
                for (Map.Entry<TierSource, SavedEntry> site : player.getValue().entrySet()) {
                    SavedEntry e = site.getValue();
                    if (site.getKey() == null || e == null || now - e.fetchedAt() > DISK_MAX_AGE_MS) continue;
                    // Never overwrite something already fetched since the game started
                    perSource.putIfAbsent(site.getKey(),
                            new Entry(Optional.ofNullable(e.profile()), e.fetchedAt() + TTL_MS, false, e.fetchedAt()));
                    loaded++;
                }
            }
            QTiersLog.LOGGER.info("Loaded {} saved tier results", loaded);
        } catch (Exception e) {
            QTiersLog.LOGGER.warn("Could not read {}, starting with an empty cache", DISK_FILE, e);
        }
    }

    private static synchronized void saveToDisk() {
        long now = System.currentTimeMillis();
        List<Map.Entry<UUID, Map<TierSource, SavedEntry>>> players = new ArrayList<>();
        for (Map.Entry<UUID, Map<TierSource, Entry>> player : CACHE.entrySet()) {
            Map<TierSource, SavedEntry> sites = new EnumMap<>(TierSource.class);
            player.getValue().forEach((source, e) -> {
                if (e.fetchedAt() > 0 && now - e.fetchedAt() <= DISK_MAX_AGE_MS) {
                    sites.put(source, new SavedEntry(e.profile().orElse(null), e.fetchedAt()));
                }
            });
            if (!sites.isEmpty()) players.add(Map.entry(player.getKey(), sites));
        }
        // Newest first, so the size cap drops the players not seen for longest
        players.sort(Comparator.comparingLong((Map.Entry<UUID, Map<TierSource, SavedEntry>> p) ->
                p.getValue().values().stream().mapToLong(SavedEntry::fetchedAt).max().orElse(0)).reversed());
        Map<UUID, Map<TierSource, SavedEntry>> out = new HashMap<>();
        for (var p : players.subList(0, Math.min(players.size(), DISK_MAX_PLAYERS))) out.put(p.getKey(), p.getValue());
        try {
            Files.createDirectories(DISK_FILE.getParent());
            // Write a temp file first so a crash mid-write can't corrupt the cache
            Path tmp = DISK_FILE.resolveSibling("cache.json.tmp");
            Files.writeString(tmp, GSON.toJson(out));
            Files.move(tmp, DISK_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            QTiersLog.LOGGER.warn("Could not save {}", DISK_FILE, e);
        }
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

    /** Downloads a page as text (used for leaderboards). Fails on any non-200 response. */
    public static CompletableFuture<String> fetchString(String url) {
        return HTTP.sendAsync(request(url), HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200) throw new IllegalStateException("HTTP " + response.statusCode() + " from " + url);
            return response.body();
        });
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
