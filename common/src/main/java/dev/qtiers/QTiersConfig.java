package dev.qtiers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/** Stored at config/qtiers.json. Edit in game with /qtiers. */
public final class QTiersConfig {
    public enum Position { LEFT, RIGHT, OFF }

    /** Same options as TierTagger: when to fall back to (or force) the player's highest tier. */
    public enum HighestMode { NEVER, NOT_FOUND, ALWAYS }


    public static final class SiteSettings {
        public Position position;
        public String gamemode = GameModes.HIGHEST;

        SiteSettings(Position position) {
            this.position = position;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("qtiers.json");

    private static QTiersConfig instance = new QTiersConfig();

    public boolean enabled = true;
    public boolean showInNametags = true;
    public boolean showInTabList = true;
    public boolean showIcons = true;
    /** Adds a small site tag (PVP / SUB / MC / HQ / TMC) in front of each tier. */
    public boolean showSiteLabel = false;
    /** Separator next to the name takes the tier's color instead of gray (like Tiers' dynamic separator). */
    public boolean coloredSeparator = false;
    public boolean showRetired = true;
    /** Shows your own nametag in third person (F5), like the WhoAmI mod. */
    public boolean showOwnNametag = true;
    /** Top-left box with the tiers of the last player you hit. */
    public boolean opponentHud = true;
    /** Opponent HUD position as a share (0..1) of the free space left/right and top/bottom; 0,0 = top-left corner. */
    public double opponentHudX = 0;
    public double opponentHudY = 0;
    /** Online players' names in chat open their profile when clicked. */
    public boolean clickableNames = true;
    public HighestMode highestMode = HighestMode.NOT_FOUND;

    public Map<TierSource, SiteSettings> sites = defaultSites();

    private static Map<TierSource, SiteSettings> defaultSites() {
        Map<TierSource, SiteSettings> map = new EnumMap<>(TierSource.class);
        map.put(TierSource.MCTIERS, new SiteSettings(Position.RIGHT));
        map.put(TierSource.PVPTIERS, new SiteSettings(Position.LEFT));
        map.put(TierSource.SUBTIERS, new SiteSettings(Position.RIGHT));
        map.put(TierSource.MCPVP, new SiteSettings(Position.RIGHT));
        map.put(TierSource.PVPHQ, new SiteSettings(Position.RIGHT));
        map.put(TierSource.TIERMC, new SiteSettings(Position.RIGHT));
        return map;
    }

    public SiteSettings site(TierSource source) {
        return sites.get(source);
    }

    public static QTiersConfig get() {
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                QTiersConfig loaded = GSON.fromJson(Files.readString(FILE), QTiersConfig.class);
                if (loaded != null) instance = loaded;
            }
        } catch (Exception e) {
            QTiersLog.LOGGER.warn("Could not read {}, using defaults", FILE, e);
        }
        instance.repair();
        save();
    }

    /** Fills anything missing or invalid after loading an older/hand-edited file. */
    private void repair() {
        if (highestMode == null) highestMode = HighestMode.NOT_FOUND;
        Map<TierSource, SiteSettings> defaults = defaultSites();
        Map<TierSource, SiteSettings> fixed = new EnumMap<>(TierSource.class);
        for (TierSource source : TierSource.values()) {
            SiteSettings s = sites == null ? null : sites.get(source);
            if (s == null) s = defaults.get(source);
            if (s.position == null) s.position = defaults.get(source).position;
            if (s.gamemode == null || !GameModes.cycleFor(source).contains(s.gamemode)) s.gamemode = GameModes.HIGHEST;
            fixed.put(source, s);
        }
        sites = fixed;
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (IOException e) {
            QTiersLog.LOGGER.warn("Could not write {}", FILE, e);
        }
    }
}
