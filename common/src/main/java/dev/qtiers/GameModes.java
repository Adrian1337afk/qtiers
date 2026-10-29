package dev.qtiers;


import java.util.List;
import java.util.Map;

/**
 * Gamemode keys used by each site and a text color per mode. Icons are glyphs in
 * assets/qtiers/font/&lt;site&gt;.json: U+E900 + the mode's index in that site's {@link #BY_SOURCE}
 * list (keep the font files in the same order); {@code ModeTexts} turns them into text per version.
 */
public final class GameModes {
    /** Special gamemode value meaning "show this player's highest tier on the site". */
    public static final String HIGHEST = "highest";

    public record Mode(String id, String title, int color) {}

    private static final Map<String, Mode> MODES = Map.ofEntries(
            mode("axe", "Axe", 0x55FF55),
            mode("mace", "Mace", 0xAAAAAA),
            mode("neth_pot", "Neth Pot", 0x7D4A40),
            mode("netherite_pot", "Neth Pot", 0x7D4A40),
            mode("pot", "Pot", 0xFF0000),
            mode("smp", "SMP", 0xECCB45),
            mode("sword", "Sword", 0xA4FDF0),
            mode("uhc", "UHC", 0xFF5555),
            mode("crystal", "Crystal", 0xFF55FF),
            mode("vanilla", "Vanilla", 0xFF55FF),
            mode("bed", "Bed", 0xFF0000),
            mode("bow", "Bow", 0x663D10),
            mode("creeper", "Creeper", 0x55FF55),
            mode("debuff", "DeBuff", 0x555555),
            mode("dia_crystal", "Dia Vanilla", 0x55FFFF),
            mode("dia_smp", "Dia SMP", 0x8C668B),
            mode("diamond_smp", "Dia SMP", 0x8C668B),
            mode("elytra", "Elytra", 0x8D8DB1),
            mode("manhunt", "Manhunt", 0xFF5555),
            mode("minecart", "Minecart", 0xAAAAAA),
            mode("cart", "Cart", 0xAAAAAA),
            mode("og_vanilla", "OG Vanilla", 0xFFAA00),
            mode("speed", "Speed", 0x43A9D1),
            mode("trident", "Trident", 0x579B8C),
            mode("spear", "Spear", 0x9FB8C8),
            mode("spear_mace", "Spear", 0x9FB8C8),
            mode("shield", "Shield", 0xB08D57),
            mode("early_game", "Early Game", 0x7FD36B),
            mode("late_game", "Late Game", 0xE0A040),
            mode("end_game", "End Game", 0xB070E0),
            mode("potion", "Pot", 0xFF0000),
            mode("nethpot", "Neth Pot", 0x7D4A40),
            mode("nethop", "Neth Pot", 0x7D4A40));

    private static final Map<TierSource, List<String>> BY_SOURCE = Map.of(
            TierSource.MCTIERS, List.of("vanilla", "uhc", "pot", "nethop", "smp", "sword", "axe", "mace", "spear"),
            TierSource.PVPTIERS, List.of("crystal", "sword", "uhc", "pot", "neth_pot", "smp", "axe", "mace"),
            TierSource.SUBTIERS, List.of("minecart", "dia_crystal", "debuff", "elytra", "speed", "creeper",
                    "manhunt", "dia_smp", "bow", "bed", "og_vanilla", "trident"),
            TierSource.MCPVP, List.of("sword", "shield", "pot", "early_game", "end_game", "mace", "late_game",
                    "spear", "diamond_smp", "netherite_pot", "creeper", "cart", "bow", "smp", "crystal", "uhc"),
            TierSource.PVPHQ, List.of("sword", "axe", "mace", "spear_mace", "uhc", "netherite_pot", "pot", "smp",
                    "diamond_smp", "vanilla", "cart"),
            TierSource.TIERMC, List.of("vanilla", "sword", "mace", "uhc", "potion", "nethpot", "axe", "smp"));

    private GameModes() {}

    private static Map.Entry<String, Mode> mode(String id, String title, int color) {
        return Map.entry(id, new Mode(id, title, color));
    }

    public static Mode get(String id) {
        Mode mode = MODES.get(id);
        return mode != null ? mode : new Mode(id, id, 0xFFFFFF);
    }

    /** "highest" followed by every gamemode the site ranks. */
    public static List<String> cycleFor(TierSource source) {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(HIGHEST), BY_SOURCE.get(source).stream()).toList();
    }

    public static String next(TierSource source, String current) {
        List<String> modes = cycleFor(source);
        int i = modes.indexOf(current);
        return modes.get((i + 1) % modes.size());
    }

    /** Position of a gamemode in the site's icon font (U+E900 + index), or -1 if the site doesn't list it. */
    public static int iconIndex(TierSource source, String id) {
        return BY_SOURCE.get(source).indexOf(id);
    }
}
