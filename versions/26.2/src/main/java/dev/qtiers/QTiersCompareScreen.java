package dev.qtiers;

import dev.qtiers.TierProfile.Ranking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** /qtiers compare a b: two players' tiers side by side, site by site, with the better tier marked. */
public class QTiersCompareScreen extends QTiersListScreen {
    private enum State { LOADING, FAILED, DONE }

    /** A site heading, or one gamemode with both players' tiers. */
    private record Row(TierSource source, @Nullable String mode, Optional<Ranking> a, Optional<Ranking> b, Component note) {}

    private final String nameA, nameB;
    private final Map<TierSource, State> stateA = new EnumMap<>(TierSource.class), stateB = new EnumMap<>(TierSource.class);
    private final Map<TierSource, TierProfile> profileA = new EnumMap<>(TierSource.class), profileB = new EnumMap<>(TierSource.class);
    private List<Row> rows = List.of();

    public QTiersCompareScreen(String nameA, String nameB, @Nullable Screen parent) {
        super(Component.literal(nameA + "  vs  " + nameB), parent);
        this.nameA = nameA;
        this.nameB = nameB;
        for (TierSource s : TierSource.values()) {
            stateA.put(s, State.LOADING);
            stateB.put(s, State.LOADING);
        }
        load(nameA, stateA, profileA);
        load(nameB, stateB, profileB);
        rebuild();
    }

    private void load(String name, Map<TierSource, State> states, Map<TierSource, TierProfile> profiles) {
        TierApi.lookupEach(name).forEach((source, future) -> future.whenComplete((result, err) ->
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    states.put(source, err != null ? State.FAILED : State.DONE);
                    if (err == null) result.ifPresent(p -> profiles.put(source, p));
                    rebuild();
                })));
    }

    private void rebuild() {
        List<Row> list = new ArrayList<>();
        for (TierSource source : TierSource.values()) {
            TierProfile a = profileA.get(source), b = profileB.get(source);
            Component note = note(stateA.get(source), a).copy().append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(note(stateB.get(source), b));
            list.add(new Row(source, null, Optional.empty(), Optional.empty(), note));
            // Every gamemode either player is ranked in, best tiers first
            Set<String> modes = new LinkedHashSet<>();
            List<Ranking> all = new ArrayList<>();
            if (a != null) all.addAll(a.rankings().values());
            if (b != null) all.addAll(b.rankings().values());
            all.sort(Comparator.comparingInt(Ranking::score));
            all.forEach(r -> modes.add(r.mode()));
            for (String mode : modes) {
                list.add(new Row(source, mode, a == null ? Optional.empty() : a.get(mode),
                        b == null ? Optional.empty() : b.get(mode), Component.empty()));
            }
        }
        rows = list;
    }

    private static Component note(State state, @Nullable TierProfile profile) {
        return switch (state) {
            case LOADING -> Component.literal("Loading...").withStyle(ChatFormatting.GRAY);
            case FAILED -> Component.literal("Couldn't load").withStyle(s -> s.withColor(0xFF7777));
            case DONE -> profile == null || !profile.isRanked() ? Component.literal("Not ranked").withStyle(ChatFormatting.DARK_GRAY)
                    : Component.literal(profile.summary().isEmpty() ? "Ranked" : profile.summary()).withStyle(ChatFormatting.GRAY);
        };
    }

    private int colA(int x, int width) {
        return x + width / 2;
    }

    private int colB(int x, int width) {
        return x + width * 3 / 4;
    }

    @Override
    protected void renderHeader(GuiGraphicsExtractor context, int x, int y, int width) {
        drawClipped(context, Component.literal(nameA).withStyle(ChatFormatting.BOLD), colA(x, width), y, width / 4 - 4, 0xFFFFFFFF);
        drawClipped(context, Component.literal(nameB).withStyle(ChatFormatting.BOLD), colB(x, width), y, width / 4 - 4, 0xFFFFFFFF);
    }

    @Override
    protected int rowCount() {
        return rows.size();
    }

    @Override
    protected void renderRow(GuiGraphicsExtractor context, int index, int x, int y, int width, boolean hovered) {
        Row row = rows.get(index);
        if (row.mode() == null) {
            context.fill(x - 2, y - 2, x + width + 2, y + ROW_H - 2, 0x40000000 | (row.source().color & 0xFFFFFF));
            context.text(font, Component.literal(row.source().displayName)
                    .withStyle(s -> s.withColor(row.source().color).withBold(true)), x + 2, y, 0xFFFFFFFF);
            drawClipped(context, row.note(), colA(x, width), y, width / 2 - 4, 0xFFFFFFFF);
            return;
        }
        drawClipped(context, ModeTexts.styledName(row.source(), row.mode()), x + 8, y, width / 2 - 12, 0xFFFFFFFF);
        // Lower score is better; a retired tier never beats an active one
        int a = row.a().map(r -> r.score() + (r.retired() ? 1000 : 0)).orElse(Integer.MAX_VALUE);
        int b = row.b().map(r -> r.score() + (r.retired() ? 1000 : 0)).orElse(Integer.MAX_VALUE);
        // Only mark a winner when both are ranked in this gamemode
        boolean both = row.a().isPresent() && row.b().isPresent();
        context.text(font, cell(row.a(), both && a < b), colA(x, width), y, 0xFFFFFFFF);
        context.text(font, cell(row.b(), both && b < a), colB(x, width), y, 0xFFFFFFFF);
    }

    private static Component cell(Optional<Ranking> ranking, boolean better) {
        if (ranking.isEmpty()) return Component.literal("-").withStyle(s -> s.withColor(0x555555));
        Ranking r = ranking.get();
        MutableComponent t = Component.literal(r.label()).withStyle(s -> s.withColor(r.color()));
        return better ? t.append(Component.literal(" ▲").withStyle(s -> s.withColor(0x55FF55))) : t;
    }
}
