package dev.qtiers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Support for servers that draw their own names instead of vanilla's (e.g. mcpvp.club):
 * nametags made of text displays, and tab lists made of fake entries
 * whose text contains the real player's name.
 */
public final class ServerNames {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Pattern NOT_NAME_CHAR = Pattern.compile("[^A-Za-z0-9_]+");

    private static Map<String, UUID> namesToIds = Map.of();
    private static long namesBuiltAt;

    private ServerNames() {}

    /**
     * Any text display whose text contains a known player's name gets that player's tiers, like the
     * Tiers mod. Riding is not required: some servers move the nametag display with the player instead.
     */
    public static Text decorateTextDisplay(Text text) {
        QTiersConfig config = QTiersConfig.get();
        if (text == null || !config.enabled || !config.showInNametags) return text;
        UUID id = playerIn(text.getString());
        return id == null ? text : TierDisplay.decorate(id, text);
    }

    /**
     * The real player behind a tab entry. Normal entries are the player themselves; a fake entry
     * (blank or invalid profile name) is matched by a real player's name appearing in its text.
     */
    public static UUID tabPlayer(PlayerListEntry entry) {
        if (isRealPlayer(entry)) return entry.getProfile().id();
        Text display = entry.getDisplayName();
        if (display == null) return null;
        return playerIn(display.getString());
    }

    /** The first known player whose name appears as a word in the text, or null. */
    private static UUID playerIn(String text) {
        Map<String, UUID> names = names();
        for (String word : NOT_NAME_CHAR.split(text)) {
            UUID id = names.get(word.toLowerCase(Locale.ROOT));
            if (id != null) return id;
        }
        return null;
    }

    /** True for entries that are real players (not a server's fake tab rows). */
    public static boolean isRealPlayer(PlayerListEntry entry) {
        String name = entry.getProfile().name();
        return name != null && NAME.matcher(name).matches();
    }

    /** Lowercase name to uuid of every real player the server told us about (listed or not), rebuilt each second. */
    private static Map<String, UUID> names() {
        long now = System.currentTimeMillis();
        if (now - namesBuiltAt > 1000) {
            Map<String, UUID> map = new HashMap<>();
            var handler = MinecraftClient.getInstance().getNetworkHandler();
            if (handler != null) {
                for (PlayerListEntry e : handler.getPlayerList()) {
                    if (isRealPlayer(e)) map.put(e.getProfile().name().toLowerCase(Locale.ROOT), e.getProfile().id());
                }
            }
            namesToIds = map;
            namesBuiltAt = now;
        }
        return namesToIds;
    }

}
