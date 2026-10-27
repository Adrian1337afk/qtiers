package dev.qtiers;

import dev.qtiers.QTiersConfig.HighestMode;
import dev.qtiers.QTiersConfig.Position;
import dev.qtiers.TierProfile.Ranking;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Builds "[MC tier] | Name | [PvP tier] [Sub tier]" for nametags and the tab list. */
public final class TierDisplay {
    private TierDisplay() {}

    private record Tag(Component text, int color) {}

    public static Component decorate(UUID uuid, Component name) {
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

        MutableComponent out = Component.empty();
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

    private static Component tagText(TierSource source, Ranking ranking, QTiersConfig config) {
        MutableComponent text = Component.empty();
        if (config.showSiteLabel) {
            text.append(Component.literal(source.shortName + " ").withStyle(s -> s.withColor(source.color)));
        }
        if (config.showIcons) {
            text.append(ModeTexts.icon(source, ranking.mode()));
        }
        text.append(Component.literal(ranking.label()).withStyle(s -> s.withColor(ranking.color())));
        return text;
    }

    private static Component join(List<Tag> tags) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) out.append(Component.literal(" "));
            out.append(tags.get(i).text());
        }
        return out;
    }

    private static Component separator(int tierColor, QTiersConfig config) {
        MutableComponent sep = Component.literal(" | ");
        return config.coloredSeparator ? sep.withStyle(s -> s.withColor(tierColor)) : sep.withStyle(ChatFormatting.GRAY);
    }
}
