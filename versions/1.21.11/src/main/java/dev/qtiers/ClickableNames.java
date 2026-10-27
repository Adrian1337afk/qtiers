package dev.qtiers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes online players' names in chat clickable: clicking opens their QTiers profile.
 * The click runs "/qtiers <name>"; {@code ChatScreenMixin} catches it and opens the screen directly.
 */
public final class ClickableNames {
    public static final String COMMAND_PREFIX = "/qtiers ";

    private ClickableNames() {}

    public static Text process(Text message) {
        if (!QTiersConfig.get().enabled || !QTiersConfig.get().clickableNames || message == null) return message;
        var handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) return message;

        // Only names that actually appear in this message go into the pattern
        String plain = message.getString().toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (PlayerListEntry entry : handler.getPlayerList()) {
            String name = entry.getProfile().name();
            if (name.length() >= 3 && plain.contains(name.toLowerCase(Locale.ROOT))) names.add(Pattern.quote(name));
        }
        if (names.isEmpty()) return message;
        Pattern pattern = Pattern.compile("(?<![A-Za-z0-9_])(" + String.join("|", names) + ")(?![A-Za-z0-9_])",
                Pattern.CASE_INSENSITIVE);

        // Rebuild the message segment by segment, keeping every segment's style
        MutableText out = Text.empty();
        boolean[] changed = {false};
        message.visit((style, segment) -> {
            Matcher m = pattern.matcher(segment);
            int last = 0;
            while (m.find()) {
                if (m.start() > last) out.append(Text.literal(segment.substring(last, m.start())).setStyle(style));
                out.append(Text.literal(m.group(1)).setStyle(clickable(style, m.group(1))));
                last = m.end();
                changed[0] = true;
            }
            if (last < segment.length()) out.append(Text.literal(segment.substring(last)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return changed[0] ? out : message;
    }

    private static Style clickable(Style style, String name) {
        // Links and other server click actions stay as they are; vanilla's "/tell" suggestion is replaced
        if (style.getClickEvent() != null && !(style.getClickEvent() instanceof ClickEvent.SuggestCommand)) return style;
        return style.withClickEvent(new ClickEvent.RunCommand(COMMAND_PREFIX + name))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to see " + name + "'s tiers")
                        .styled(s -> s.withColor(0xE8BA3A))));
    }
}
