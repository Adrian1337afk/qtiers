package dev.qtiers;

import java.util.UUID;

/** The three tier-list websites and how to reach their public APIs. */
public enum TierSource {
    MCTIERS("MCTiers", "MC", 0x5DADEC,
            "https://mctiers.com/api/v2/profile/%s",
            "https://mctiers.com/api/v2/profile/by-name/%s"),
    PVPTIERS("PvPTiers", "PVP", 0xE0544E,
            "https://pvptiers.com/api/profile/%s",
            "https://pvptiers.com/api/search_profile/%s"),
    SUBTIERS("SubTiers", "SUB", 0x7ED957,
            "https://subtiers.net/api/v2/profile/%s",
            "https://subtiers.net/api/v2/profile/by-name/%s");

    public final String displayName;
    public final String shortName;
    public final int color;
    private final String uuidUrl;
    private final String nameUrl;

    TierSource(String displayName, String shortName, int color, String uuidUrl, String nameUrl) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.color = color;
        this.uuidUrl = uuidUrl;
        this.nameUrl = nameUrl;
    }

    public String urlFor(UUID uuid) {
        return uuidUrl.formatted(uuid.toString().replace("-", ""));
    }

    public String urlFor(String playerName) {
        return nameUrl.formatted(playerName);
    }
}
