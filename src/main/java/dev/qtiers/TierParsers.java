package dev.qtiers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qtiers.TierProfile.Ranking;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns each site's response into a {@link TierProfile}. Each parser returns empty when the player
 * isn't listed, and throws when the response isn't what the site normally sends.
 */
final class TierParsers {
    private TierParsers() {}

    @FunctionalInterface
    interface Parser {
        Optional<TierProfile> parse(int status, URI finalUri, String body);
    }

    private static void requireOk(int status, URI uri) {
        if (status != 200) throw new IllegalStateException("HTTP " + status + " from " + uri);
    }

    // ---------- PvPTiers / SubTiers: {"name", "region", "points", "overall", "rankings": {mode: {tier, pos, ...}}} ----------

    static Optional<TierProfile> tierList(int status, URI uri, String body) {
        if (status == 404) return Optional.empty();
        requireOk(status, uri);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        if (json.has("error")) return Optional.empty();

        Map<String, Ranking> rankings = new LinkedHashMap<>();
        if (json.has("rankings") && json.get("rankings").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("rankings").entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                JsonObject r = e.getValue().getAsJsonObject();
                int tier = intOr(r, "tier", 0);
                if (tier < 1 || tier > 5) continue;
                // These sites use pos 0 = High, 1 = Low
                int pos = intOr(r, "pos", 1) == 0 ? TierProfile.HIGH : TierProfile.LOW;
                int peakTier = intOr(r, "peak_tier", tier);
                int peakPos = intOr(r, "peak_pos", intOr(r, "pos", 1)) == 0 ? TierProfile.HIGH : TierProfile.LOW;
                rankings.put(e.getKey(), new Ranking(e.getKey(), tier, pos, peakTier, peakPos, boolOr(r, "retired")));
            }
        }
        String summary = "#" + intOr(json, "overall", 0) + " · " + intOr(json, "points", 0) + " pts";
        return Optional.of(new TierProfile(strOr(json, "name", ""), summary, region(strOr(json, "region", "")), rankings));
    }

    // ---------- PVPHQ: {"name", "regions": [...], "ranked": [{gametype, tier: "LT3", unranked, inactive, rating}], "best": {...}} ----------

    static Optional<TierProfile> pvphq(int status, URI uri, String body) {
        if (status == 404) return Optional.empty();
        requireOk(status, uri);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();

        Map<String, Ranking> rankings = new LinkedHashMap<>();
        if (json.has("ranked") && json.get("ranked").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("ranked")) {
                if (!el.isJsonObject()) continue;
                JsonObject r = el.getAsJsonObject();
                if (boolOr(r, "unranked")) continue;
                int[] code = Ranking.parseCode(strOr(r, "tier", null));
                String mode = strOr(r, "gametype", null);
                if (code == null || mode == null) continue;
                // Inactive players keep their tier on PVPHQ; show it like a retired tier
                rankings.put(mode, new Ranking(mode, code[0], code[1], code[0], code[1], boolOr(r, "inactive")));
            }
        }
        String summary = "";
        if (json.has("best") && json.get("best").isJsonObject()) {
            int rating = intOr(json.getAsJsonObject("best"), "rating", 0);
            if (rating > 0) summary = "Best rating " + rating;
        }
        String region = "";
        if (json.has("regions") && json.get("regions").isJsonArray()) {
            JsonArray regions = json.getAsJsonArray("regions");
            if (!regions.isEmpty() && regions.get(0).isJsonPrimitive()) region = regions.get(0).getAsString();
        }
        return Optional.of(new TierProfile(strOr(json, "name", ""), summary, region(region), rankings));
    }

    // ---------- MCPVP: no API, so read the public profile page (www.mcpvp.com/@name) ----------

    private static final Pattern MCPVP_TITLE = Pattern.compile("<title>\\s*MCPVP\\s*\\|\\s*([^<]+?)\\s*</title>");
    private static final Pattern MCPVP_KIT = Pattern.compile(
            "<button class=\"ranking-kit\"[^>]*?data-kit=\"([^\"]+)\"[^>]*>(.*?)</button>", Pattern.DOTALL);
    private static final Pattern TIER_CODE = Pattern.compile("data-tier-code=\"([^\"]+)\"");
    private static final Pattern MCPVP_OVERALL = Pattern.compile("class=\"overall-tier[^\"]*\"[^>]*data-tier-code=\"([^\"]+)\"");
    private static final Pattern NUMBER = Pattern.compile("^[0-9][0-9,]*(\\.[0-9]+)?$");

    static Optional<TierProfile> mcpvp(int status, URI uri, String body) {
        requireOk(status, uri);
        // Unknown players are redirected to the home page instead of a 404
        if (uri.getPath() == null || !uri.getPath().startsWith("/@")) return Optional.empty();

        Matcher title = MCPVP_TITLE.matcher(body);
        String name = title.find() ? title.group(1) : uri.getPath().substring(2);

        Map<String, Ranking> rankings = new LinkedHashMap<>();
        Matcher kit = MCPVP_KIT.matcher(body);
        while (kit.find()) {
            String mode = kit.group(1);
            if (mode.equals("overall")) continue;
            Matcher codeMatch = TIER_CODE.matcher(kit.group(2));
            int[] code = codeMatch.find() ? Ranking.parseCode(codeMatch.group(1)) : null;
            if (code == null) continue; // "???" = hidden tier
            rankings.putIfAbsent(mode, new Ranking(mode, code[0], code[1], code[0], code[1], false));
        }

        String summary = "";
        Matcher overall = MCPVP_OVERALL.matcher(body);
        if (overall.find() && Ranking.parseCode(overall.group(1)) != null) {
            summary = "Overall " + overall.group(1).toUpperCase();
            // The points total is the first number shown after the overall tier ("HT2 293.5 / 300 Points")
            String after = body.substring(overall.end(), Math.min(body.length(), overall.end() + 1500));
            for (String token : after.replaceAll("<[^>]+>", " ").trim().split("\\s+")) {
                if (NUMBER.matcher(token).matches()) {
                    summary += " · " + token + " pts";
                    break;
                }
            }
        }
        return Optional.of(new TierProfile(name, summary, "", rankings));
    }

    // ---------- helpers ----------

    private static String region(String region) {
        return region == null || region.isBlank() || region.equals("??") ? "" : region;
    }

    private static int intOr(JsonObject o, String key, int fallback) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? fallback : e.getAsInt();
    }

    private static String strOr(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? fallback : e.getAsString();
    }

    private static boolean boolOr(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && !e.isJsonNull() && e.getAsBoolean();
    }
}
