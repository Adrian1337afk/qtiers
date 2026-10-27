package dev.qtiers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;

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

    public static Component process(Component message) {
        if (!QTiersConfig.get().enabled || !QTiersConfig.get().clickableNames || message == null) return message;
        var handler = Minecraft.getInstance().getConnection();
        if (handler == null) return message;

        // Only names that actually appear in this message go into the pattern
        String plain = message.getString().toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (PlayerInfo entry : handler.getOnlinePlayers()) {
            String name = entry.getProfile().name();
            if (name.length() >= 3 && plain.contains(name.toLowerCase(Locale.ROOT))) names.add(Pattern.quote(name));
        }
        if (names.isEmpty()) return message;
        Pattern pattern = Pattern.compile("(?<![A-Za-z0-9_])(" + String.join("|", names) + ")(?![A-Za-z0-9_])",
                Pattern.CASE_INSENSITIVE);

        // Rebuild the message segment by segment, keeping every segment's style
        MutableComponent out = Component.empty();
        boolean[] changed = {false};
        message.visit((style, segment) -> {
            Matcher m = pattern.matcher(segment);
            int last = 0;
            while (m.find()) {
                if (m.start() > last) out.append(Component.literal(segment.substring(last, m.start())).setStyle(style));
                out.append(Component.literal(m.group(1)).setStyle(clickable(style, m.group(1))));
                last = m.end();
                changed[0] = true;
            }
            if (last < segment.length()) out.append(Component.literal(segment.substring(last)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return changed[0] ? out : message;
    }

    private static Style clickable(Style style, String name) {
        // Links and other server click actions stay as they are; vanilla's "/tell" suggestion is replaced
        if (style.getClickEvent() != null && !(style.getClickEvent() instanceof ClickEvent.SuggestCommand)) return style;
        return style.withClickEvent(new ClickEvent.RunCommand(COMMAND_PREFIX + name))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("MouseButtonEvent to see " + name + "'s tiers")
                        .withStyle(s -> s.withColor(0xE8BA3A))));
    }
}
