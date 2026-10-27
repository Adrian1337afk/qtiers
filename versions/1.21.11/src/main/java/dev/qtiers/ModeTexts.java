package dev.qtiers;

import net.minecraft.text.MutableText;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

/** Gamemode icons (each site's own icon font) and names as Minecraft text. */
public final class ModeTexts {
    private ModeTexts() {}

    /** The gamemode's icon from the site's own icon set; a dot for modes the site doesn't list. */
    public static Text icon(TierSource source, String id) {
        int index = GameModes.iconIndex(source, id);
        if (index < 0) return Text.literal("•");
        StyleSpriteSource font = new StyleSpriteSource.Font(
                Identifier.of(QTiers.MOD_ID, source.name().toLowerCase(Locale.ROOT)));
        return Text.literal(String.valueOf((char) (0xE900 + index))).styled(s -> s.withColor(0xFFFFFF).withFont(font));
    }

    /** Icon + title in the mode's color, e.g. for chat messages and buttons. */
    public static MutableText styledName(TierSource source, String id) {
        if (GameModes.HIGHEST.equals(id)) return Text.literal("Highest").styled(s -> s.withColor(0xFFD166));
        GameModes.Mode mode = GameModes.get(id);
        return Text.empty().append(icon(source, id)).append(" ")
                .append(Text.literal(mode.title()).styled(s -> s.withColor(mode.color())));
    }
}
