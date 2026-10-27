package dev.qtiers;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/** Gamemode icons (each site's own icon font) and names as Minecraft text. */
public final class ModeTexts {
    private ModeTexts() {}

    /** The gamemode's icon from the site's own icon set; a dot for modes the site doesn't list. */
    public static Component icon(TierSource source, String id) {
        int index = GameModes.iconIndex(source, id);
        if (index < 0) return Component.literal("•");
        FontDescription font = new FontDescription.Resource(
                Identifier.fromNamespaceAndPath(QTiers.MOD_ID, source.name().toLowerCase(Locale.ROOT)));
        return Component.literal(String.valueOf((char) (0xE900 + index))).withStyle(s -> s.withColor(0xFFFFFF).withFont(font));
    }

    /** Icon + title in the mode's color, e.g. for chat messages and buttons. */
    public static MutableComponent styledName(TierSource source, String id) {
        if (GameModes.HIGHEST.equals(id)) return Component.literal("Highest").withStyle(s -> s.withColor(0xFFD166));
        GameModes.Mode mode = GameModes.get(id);
        return Component.empty().append(icon(source, id)).append(" ")
                .append(Component.literal(mode.title()).withStyle(s -> s.withColor(mode.color())));
    }
}
