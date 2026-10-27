package dev.qtiers;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

/**
 * A player's rankings on one tier-list site, already converted from that site's format
 * (see {@link TierParsers}). {@code summary} is the site's stats line for the profile screen.
 */
public record TierProfile(String name, String summary, String region, Map<String, Ranking> rankings) {

    /** Tier positions: High, Mid, Low, and mcpvp.com's "S" (ST6, the lowest tier). */
    public static final int HIGH = 0, MID = 1, LOW = 2, SUB = 3;
    private static final String POSITIONS = "HMLS";

    /** One gamemode ranking, e.g. tier 3 / pos MID = MT3. */
    public record Ranking(String mode, int tier, int pos, int peakTier, int peakPos, boolean retired) {
        public static final int RETIRED_COLOR = 0xA2D6FF;

        /** Retired/inactive players are shown with their peak tier and an R prefix, as on the sites. */
        public String label() {
            return retired ? "R" + tierLabel(peakTier, peakPos) : tierLabel(tier, pos);
        }

        public int color() {
            return retired ? RETIRED_COLOR : tierColor(tier, pos);
        }

        public boolean hasHigherPeak() {
            return !retired && rank(peakTier, peakPos) < rank(tier, pos);
        }

        /** Lower is better: HT1 = 0, MT1 = 1, LT1 = 2, HT2 = 4, ... */
        public int score() {
            return rank(tier, pos);
        }

        private static int rank(int tier, int pos) {
            return (tier - 1) * 4 + pos;
        }

        public static String tierLabel(int tier, int pos) {
            return POSITIONS.charAt(pos) + "T" + tier;
        }

        /** mcpvp.com's tier palette (the [data-tier-code] colors on its pages). */
        public static int tierColor(int tier, int pos) {
            if (pos == SUB) return 0x383838; // ST6
            return switch (tier * 3 + Math.min(pos, LOW)) {
                case 3 -> 0xFFDA55;  // HT1
                case 4 -> 0xE8BE3C;  // MT1
                case 5 -> 0xC4A34B;  // LT1
                case 6 -> 0x9EB3D1;  // HT2
                case 7 -> 0x91A0B7;  // MT2
                case 8 -> 0x858D9C;  // LT2
                case 9 -> 0xEB9138;  // HT3
                case 10 -> 0xD47D32; // MT3
                case 11 -> 0xBD6B2B; // LT3
                case 12 -> 0x9FD27F; // HT4
                case 13 -> 0x89BD68; // MT4
                case 14 -> 0x739F57; // LT4
                case 15 -> 0x4974B5; // HT5
                case 16 -> 0x385C94; // MT5
                case 17 -> 0x284775; // LT5
                case 18 -> 0x4C3B30; // HT6
                case 19 -> 0x45352B; // MT6
                case 20 -> 0x36291F; // LT6
                default -> 0x383838;
            };
        }

        /** Parses "HT3", "MT1", "LT5", "ST6" (any case); null for anything else, e.g. "Unranked" or "???". */
        public static int[] parseCode(String code) {
            if (code == null) return null;
            String c = code.trim().toUpperCase();
            if (c.length() != 3 || c.charAt(1) != 'T') return null;
            int pos = POSITIONS.indexOf(c.charAt(0));
            int tier = c.charAt(2) - '0';
            if (pos < 0 || tier < 1 || tier > 6) return null;
            return new int[]{tier, pos};
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
}
