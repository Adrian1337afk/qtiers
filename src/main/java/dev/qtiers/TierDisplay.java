package dev.qtiers;

import dev.qtiers.QTiersConfig.HighestMode;
import dev.qtiers.QTiersConfig.Position;
import dev.qtiers.TierProfile.Ranking;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Builds "[MC tier] | Name | [PvP tier] [Sub tier]" for nametags and the tab list. */
public final class TierDisplay {
    private TierDisplay() {}

    private record Tag(Text text, int color) {}

    public static Text decorate(UUID uuid, Text name) {
        QTiersConfig config = QTiersConfig.get();
        if (!config.enabled || uuid == null || name == null) return name;

        List<Tag> left = new ArrayList<>();
        List<Tag> right = new ArrayList<>();
        for (TierSource source : TierSource.values()) {
            QTiersConfig.SiteSettings site = config.site(source);
            if (site.position == Position.OFF) continue;
            Optional<Tag> tag = TierApi.getCached(uuid, source)
                    .flatMap(profile -> pick(profile, site.gamemode, config))
                    .map(ranking -> new Tag(tagText(source, ranking, config), ranking.color()));
            tag.ifPresent(t -> (site.position == Position.LEFT ? left : right).add(t));
        }
        if (left.isEmpty() && right.isEmpty()) return name;

        MutableText out = Text.empty();
        if (!left.isEmpty()) {
            out.append(join(left)).append(separator(left.getLast().color(), config));
        }
        out.append(name);
        if (!right.isEmpty()) {
            out.append(separator(right.getFirst().color(), config)).append(join(right));
        }
        return out;
    }

    /** Chooses which ranking to show, following TierTagger's "highest" rules. */
    public static Optional<Ranking> pick(TierProfile profile, String gamemode, QTiersConfig config) {
        Optional<Ranking> highest = profile.best(config.showRetired);
        if (GameModes.HIGHEST.equals(gamemode) || config.highestMode == HighestMode.ALWAYS) return highest;

        Optional<Ranking> selected = profile.get(gamemode).filter(r -> config.showRetired || !r.retired());
        if (selected.isEmpty() && config.highestMode == HighestMode.NOT_FOUND) return highest;
        return selected;
    }

    private static Text tagText(TierSource source, Ranking ranking, QTiersConfig config) {
        MutableText text = Text.empty();
        if (config.showSiteLabel) {
            text.append(Text.literal(source.shortName + " ").styled(s -> s.withColor(source.color)));
        }
        if (config.showIcons) {
            text.append(GameModes.icon(ranking.mode()));
        }
        text.append(Text.literal(ranking.label()).styled(s -> s.withColor(ranking.color())));
        return text;
    }

    private static Text join(List<Tag> tags) {
        MutableText out = Text.empty();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) out.append(Text.literal(" "));
            out.append(tags.get(i).text());
        }
        return out;
    }

    private static Text separator(int tierColor, QTiersConfig config) {
        MutableText sep = Text.literal(" | ");
        return config.coloredSeparator ? sep.styled(s -> s.withColor(tierColor)) : sep.formatted(Formatting.GRAY);
    }
}
