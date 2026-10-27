package dev.qtiers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Support for servers that draw their own names instead of vanilla's (e.g. mcpvp.club):
 * nametags made of text displays riding the player, and tab lists made of fake entries
 * whose text contains the real player's name.
 */
public final class ServerNames {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final Pattern NOT_NAME_CHAR = Pattern.compile("[^A-Za-z0-9_]+");

    private static Map<String, UUID> namesToIds = Map.of();
    private static long namesBuiltAt;

    private ServerNames() {}

    /** A text display riding a player whose text contains that player's name gets their tiers. */
    public static Component decorateTextDisplay(Entity display, Component text) {
        QTiersConfig config = QTiersConfig.get();
        if (text == null || !config.enabled || !config.showInNametags) return text;
        if (!(display.getVehicle() instanceof Player player)) return text;
        if (!containsWord(text.getString(), player.getGameProfile().name())) return text;
        return TierDisplay.decorate(player.getUUID(), text);
    }

    /**
     * The real player behind a tab entry. Normal entries are the player themselves; a fake entry
     * (blank or invalid profile name) is matched by a real player's name appearing in its text.
     */
    public static UUID tabPlayer(PlayerInfo entry) {
        if (isRealPlayer(entry)) return entry.getProfile().id();
        Component display = entry.getTabListDisplayName();
        if (display == null) return null;
        Map<String, UUID> names = names();
        for (String word : NOT_NAME_CHAR.split(display.getString())) {
            UUID id = names.get(word.toLowerCase(Locale.ROOT));
            if (id != null) return id;
        }
        return null;
    }

    /** True for entries that are real players (not a server's fake tab rows). */
    public static boolean isRealPlayer(PlayerInfo entry) {
        String name = entry.getProfile().name();
        return name != null && NAME.matcher(name).matches();
    }

    /** Lowercase name to uuid of every real player the server told us about (listed or not), rebuilt each second. */
    private static Map<String, UUID> names() {
        long now = System.currentTimeMillis();
        if (now - namesBuiltAt > 1000) {
            Map<String, UUID> map = new HashMap<>();
            var handler = Minecraft.getInstance().getConnection();
            if (handler != null) {
                for (PlayerInfo e : handler.getOnlinePlayers()) {
                    if (isRealPlayer(e)) map.put(e.getProfile().name().toLowerCase(Locale.ROOT), e.getProfile().id());
                }
            }
            namesToIds = map;
            namesBuiltAt = now;
        }
        return namesToIds;
    }

    private static boolean containsWord(String text, String name) {
        for (String word : NOT_NAME_CHAR.split(text)) {
            if (word.equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
