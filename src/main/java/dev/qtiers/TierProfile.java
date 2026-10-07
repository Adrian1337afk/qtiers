package dev.qtiers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** A player's rankings on one tier-list site. Every supported site uses this JSON schema. */
public record TierProfile(String name, String region, int points, int overall, Map<String, Ranking> rankings) {

    public static final TierProfile EMPTY = new TierProfile("", "", 0, 0, Map.of());

    /** One gamemode ranking. {@code pos} 0 = High, 1 = Low (HT3 / LT3). */
    public record Ranking(String mode, int tier, int pos, int peakTier, int peakPos, boolean retired) {
        public static final int RETIRED_COLOR = 0xA2D6FF;

        /** Retired players are shown with their peak tier and an R prefix, as on the sites. */
        public String label() {
            return retired ? "R" + tierLabel(peakTier, peakPos) : tierLabel(tier, pos);
        }

        public int color() {
            return retired ? RETIRED_COLOR : tierColor(tier, pos);
        }

        public boolean hasHigherPeak() {
            return !retired && peakTier * 2 + peakPos < tier * 2 + pos;
        }

        /** Lower is better: HT1 = 0, LT1 = 1, HT2 = 2, ... */
        public int score() {
            return (tier - 1) * 2 + pos;
        }

        public static String tierLabel(int tier, int pos) {
            return (pos == 0 ? "HT" : "LT") + tier;
        }

        /** MCTiers' own tier colors, taken from the site's stylesheet (--htN-foreground / --ltN-foreground). */
        public static int tierColor(int tier, int pos) {
            return switch (tier * 2 + pos) {
                case 2 -> 0xE8BA3A; // HT1
                case 3 -> 0xD5B355; // LT1
                case 4 -> 0xC4D3E7; // HT2
                case 5 -> 0xA0A7B2; // LT2
                case 6 -> 0xF89F5A; // HT3
                case 7 -> 0xC67B42; // LT3
                case 8 -> 0x81749A; // HT4
                case 9 -> 0x655B79; // LT4
                case 10 -> 0x8F82A8; // HT5
                default -> 0x655B79; // LT5
            };
        }
    }

    public boolean isRanked() {
        return !rankings.isEmpty();
    }

    /** Best ranking, preferring active over retired when the tier is equal. */
    public Optional<Ranking> best(boolean includeRetired) {
        return rankings.values().stream()
                .filter(r -> includeRetired || !r.retired())
                .min(Comparator.comparingInt(Ranking::score).thenComparing(Ranking::retired));
    }

    public Optional<Ranking> get(String mode) {
        return Optional.ofNullable(rankings.get(mode));
    }

    public static TierProfile parse(JsonObject json) {
        Map<String, Ranking> rankings = new LinkedHashMap<>();
        if (json.has("rankings") && json.get("rankings").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("rankings").entrySet()) {
                if (!e.getValue().isJsonObject()) continue;
                JsonObject r = e.getValue().getAsJsonObject();
                int tier = intOr(r, "tier", 0);
                if (tier < 1 || tier > 5) continue;
                rankings.put(e.getKey(), new Ranking(
                        e.getKey(),
                        tier,
                        intOr(r, "pos", 1),
                        intOr(r, "peak_tier", tier),
                        intOr(r, "peak_pos", intOr(r, "pos", 1)),
                        r.has("retired") && !r.get("retired").isJsonNull() && r.get("retired").getAsBoolean()));
            }
        }
        return new TierProfile(
                strOr(json, "name", ""),
                strOr(json, "region", ""),
                intOr(json, "points", 0),
                intOr(json, "overall", 0),
                rankings);
    }

    private static int intOr(JsonObject o, String key, int fallback) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? fallback : e.getAsInt();
    }

    private static String strOr(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? fallback : e.getAsString();
    }
}
