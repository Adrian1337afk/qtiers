package dev.qtiers;

import net.minecraft.text.MutableText;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * Gamemode keys used by each site, with icon glyphs (one font per icon style in assets/qtiers/font/,
 * all sharing the same codepoints) and a text color per mode.
 */
public final class GameModes {
    /** Special gamemode value meaning "show this player's highest tier on the site". */
    public static final String HIGHEST = "highest";

    public record Mode(String id, String title, char icon, int color) {}

    private static final Map<String, Mode> MODES = Map.ofEntries(
            mode("axe", "Axe", '', 0x55FF55),
            mode("mace", "Mace", '', 0xAAAAAA),
            mode("nethop", "Neth OP", '', 0x7D4A40),
            mode("neth_pot", "Neth Pot", '', 0x7D4A40),
            mode("pot", "Pot", '', 0xFF0000),
            mode("smp", "SMP", '', 0xECCB45),
            mode("sword", "Sword", '', 0xA4FDF0),
            mode("uhc", "UHC", '', 0xFF5555),
            mode("vanilla", "Vanilla", '', 0xFF55FF),
            mode("crystal", "Crystal", '', 0xFF55FF),
            mode("bed", "Bed", '', 0xFF0000),
            mode("bow", "Bow", '', 0x663D10),
            mode("creeper", "Creeper", '', 0x55FF55),
            mode("debuff", "DeBuff", '', 0x555555),
            mode("dia_crystal", "Dia Vanilla", '', 0x55FFFF),
            mode("dia_smp", "Dia SMP", '', 0x8C668B),
            mode("elytra", "Elytra", '', 0x8D8DB1),
            mode("manhunt", "Manhunt", '', 0xFF5555),
            mode("minecart", "Minecart", '', 0xAAAAAA),
            mode("og_vanilla", "OG Vanilla", '', 0xFFAA00),
            mode("speed", "Speed", '', 0x43A9D1),
            mode("trident", "Trident", '', 0x579B8C));

    private static final Map<TierSource, List<String>> BY_SOURCE = Map.of(
            TierSource.MCTIERS, List.of("vanilla", "uhc", "pot", "nethop", "smp", "sword", "axe", "mace"),
            TierSource.PVPTIERS, List.of("crystal", "sword", "uhc", "pot", "neth_pot", "smp", "axe", "mace"),
            TierSource.SUBTIERS, List.of("minecart", "dia_crystal", "debuff", "elytra", "speed", "creeper",
                    "manhunt", "dia_smp", "bow", "bed", "og_vanilla", "trident"));

    private GameModes() {}

    private static Map.Entry<String, Mode> mode(String id, String title, char icon, int color) {
        return Map.entry(id, new Mode(id, title, icon, color));
    }

    public static Mode get(String id) {
        Mode mode = MODES.get(id);
        return mode != null ? mode : new Mode(id, id, '•', 0xFFFFFF);
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

    /** The gamemode's icon in the configured icon style. */
    public static Text icon(String id) {
        return icon(id, QTiersConfig.get().iconStyle);
    }

    public static Text icon(String id, QTiersConfig.IconStyle style) {
        Mode mode = get(id);
        if (mode.icon() == '•') return Text.literal("•");
        StyleSpriteSource font = new StyleSpriteSource.Font(Identifier.of(QTiers.MOD_ID, style.fontName));
        return Text.literal(String.valueOf(mode.icon())).styled(s -> s.withColor(0xFFFFFF).withFont(font));
    }

    /** Icon + title in the mode's color, e.g. for chat messages and buttons. */
    public static MutableText styledName(String id) {
        if (HIGHEST.equals(id)) return Text.literal("Highest").styled(s -> s.withColor(0xFFD166));
        Mode mode = get(id);
        return Text.empty().append(icon(id)).append(" ")
                .append(Text.literal(mode.title()).styled(s -> s.withColor(mode.color())));
    }
}
