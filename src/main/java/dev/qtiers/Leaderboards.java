package dev.qtiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qtiers.TierProfile.Ranking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Top players per site and gamemode, for the leaderboard screen. Only the sites with a public
 * ranking API are listed: PVPHQ has no leaderboard endpoint and MCPVP has no API at all.
 */
final class Leaderboards {
    private Leaderboards() {}

    static final String OVERALL = "overall";
    static final List<TierSource> SITES = List.of(TierSource.PVPTIERS, TierSource.SUBTIERS, TierSource.TIERMC);
    private static final int PER_TIER = 25;

    /** One leaderboard line. {@code label} is a tier (e.g. HT2) or "#3"; {@code extra} is e.g. "245 pts". */
    record Row(String name, String label, int color, String extra) {}

    static CompletableFuture<List<Row>> fetch(TierSource site, String mode) {
        boolean overall = OVERALL.equals(mode);
        String url = switch (site) {
            case PVPTIERS -> "https://pvptiers.com/api/tier/" + mode + "?count=" + (overall ? 100 : PER_TIER) + "&from=0";
            case SUBTIERS -> "https://subtiers.net/api/v2/mode/" + mode + "?count=" + (overall ? 50 : PER_TIER) + "&from=0";
            case TIERMC -> "https://tiermc.com/api/v2/mode/" + mode + "?count=" + (overall ? 50 : PER_TIER) + "&from=0";
            default -> throw new IllegalArgumentException(site + " has no leaderboard");
        };
        return TierApi.fetchString(url).thenApply(body -> {
            JsonElement json = JsonParser.parseString(body);
            return overall ? parseOverall(json) : parseMode(json, mode);
        });
    }

    /** Overall: a list of players by points, e.g. [{"name", "points", ...}]. */
    private static List<Row> parseOverall(JsonElement json) {
        List<Row> rows = new ArrayList<>();
        if (!json.isJsonArray()) return rows;
        int rank = 1;
        for (JsonElement el : json.getAsJsonArray()) {
            if (!el.isJsonObject()) continue;
            JsonObject p = el.getAsJsonObject();
            rows.add(new Row(str(p, "name"), "#" + rank++, 0xFFFFFF, intOr(p, "points", 0) + " pts"));
        }
        return rows;
    }

    /**
     * One gamemode, grouped by tier. PvPTiers sends [[tier 1 players], [tier 2 players], ...];
     * SubTiers and TierMC send {"1": [...], "2": [...]}. Each player has either a tier code
     * ("HT2", TierMC) or a tier number plus pos 0 = High / 1 = Low.
     */
    private static List<Row> parseMode(JsonElement json, String mode) {
        List<Row> rows = new ArrayList<>();
        if (json.isJsonArray()) {
            JsonArray tiers = json.getAsJsonArray();
            for (int i = 0; i < tiers.size(); i++) {
                if (tiers.get(i).isJsonArray()) addTier(rows, tiers.get(i).getAsJsonArray(), i + 1, mode);
            }
        } else if (json.isJsonObject()) {
            for (int tier = 1; tier <= 6; tier++) {
                JsonElement players = json.getAsJsonObject().get(String.valueOf(tier));
                if (players != null && players.isJsonArray()) addTier(rows, players.getAsJsonArray(), tier, mode);
            }
        }
        return rows;
    }

    private static void addTier(List<Row> rows, JsonArray players, int tierFromList, String mode) {
        List<Ranking> rankings = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (JsonElement el : players) {
            if (!el.isJsonObject()) continue;
            JsonObject p = el.getAsJsonObject();
            JsonElement tierField = p.get("tier");
            int[] code = tierField != null && tierField.isJsonPrimitive() && tierField.getAsJsonPrimitive().isString()
                    ? Ranking.parseCode(tierField.getAsString()) : null;
            int tier = code != null ? code[0] : intOr(p, "tier", tierFromList);
            int pos = code != null ? code[1] : (intOr(p, "pos", 1) == 0 ? TierProfile.HIGH : TierProfile.LOW);
            JsonElement retired = p.get("retired");
            boolean isRetired = retired != null && !retired.isJsonNull() && retired.getAsBoolean();
            rankings.add(new Ranking(mode, tier, pos, tier, pos, isRetired));
            names.add(str(p, "name"));
        }
        // High before Low inside a tier, retired players last
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < rankings.size(); i++) order.add(i);
        order.sort((a, b) -> {
            Ranking ra = rankings.get(a), rb = rankings.get(b);
            if (ra.retired() != rb.retired()) return ra.retired() ? 1 : -1;
            return Integer.compare(ra.score(), rb.score());
        });
        for (int i : order) {
            Ranking r = rankings.get(i);
            rows.add(new Row(names.get(i), r.label(), r.color(), ""));
        }
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? "?" : e.getAsString();
    }

    private static int intOr(JsonObject o, String key, int fallback) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull() || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return fallback;
        return e.getAsInt();
    }

    /** "Overall" followed by the site's gamemodes. */
    static List<String> modesFor(TierSource site) {
        List<String> modes = new ArrayList<>();
        modes.add(OVERALL);
        modes.addAll(GameModes.cycleFor(site).subList(1, GameModes.cycleFor(site).size()));
        return modes;
    }
}
