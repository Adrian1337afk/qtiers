package dev.qtiers;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** /qtiers top: the top players of a site, overall or for one gamemode. */
public class QTiersLeaderboardScreen extends QTiersListScreen {
    private TierSource site;
    private String mode;
    private List<Leaderboards.Row> rows = List.of();
    private Component status = Component.literal("Loading...");
    /** Bumped on every new request so a slow, outdated answer can't replace a newer one. */
    private int request;

    public QTiersLeaderboardScreen(@Nullable Screen parent) {
        this(parent, Leaderboards.SITES.getFirst(), Leaderboards.OVERALL);
    }

    public QTiersLeaderboardScreen(@Nullable Screen parent, TierSource site, String mode) {
        super(Component.literal("Leaderboards"), parent);
        this.site = site;
        this.mode = mode;
    }

    @Override
    protected int listTop() {
        return 62;
    }

    @Override
    protected void init() {
        super.init();
        int left = width / 2 - 152;
        addRenderableWidget(CycleButton.<TierSource>builder(
                        s -> Component.literal(s.displayName).withStyle(st -> st.withColor(s.color)), site)
                .withValues(Leaderboards.SITES)
                .create(left, 24, 150, 20, Component.literal("Site"), (b, v) -> {
                    site = v;
                    mode = Leaderboards.OVERALL;
                    rebuildWidgets();
                    load();
                }));
        addRenderableWidget(CycleButton.<String>builder(this::modeName, mode)
                .withValues(Leaderboards.modesFor(site))
                .create(left + 154, 24, 150, 20, Component.literal("Mode"), (b, v) -> {
                    mode = v;
                    load();
                }));
        if (rows.isEmpty() && request == 0) load();
    }

    private Component modeName(String m) {
        return Leaderboards.OVERALL.equals(m) ? Component.literal("Overall").withStyle(s -> s.withColor(0xFFD166))
                : ModeTexts.styledName(site, m);
    }

    private void load() {
        int id = ++request;
        rows = List.of();
        status = Component.literal("Loading...");
        Leaderboards.fetch(site, mode).whenComplete((result, err) -> minecraft.execute(() -> {
            if (id != request) return;
            if (err != null) {
                QTiers.LOGGER.warn("Could not load {} leaderboard: {}", site.displayName, err.toString());
                status = Component.literal("Couldn't load " + site.displayName).withStyle(s -> s.withColor(0xFF7777));
            } else {
                rows = result;
                status = Component.literal("Nobody ranked yet");
            }
        }));
    }

    @Override
    protected int rowCount() {
        return rows.size();
    }

    @Override
    protected Component emptyText() {
        return status;
    }

    @Override
    protected void renderRow(GuiGraphicsExtractor context, int index, int x, int y, int width, boolean hovered) {
        Leaderboards.Row row = rows.get(index);
        boolean overall = Leaderboards.OVERALL.equals(mode);
        context.text(font, Component.literal((index + 1) + "."), x, y, 0xFF888888);
        int nameX = x + 28;
        if (!overall) {
            // Gamemode boards: tier label in front of the name, like a tier list
            context.text(font, Component.empty()
                    .append(ModeTexts.icon(site, mode))
                    .append(Component.literal(row.label()).withStyle(s -> s.withColor(row.color()))), nameX, y, 0xFFFFFFFF);
            nameX += 44;
        }
        drawClipped(context, Component.literal(row.name()), nameX, y, width - (nameX - x) - 60, hovered ? 0xFFFFFF55 : 0xFFFFFFFF);
        if (!row.extra().isEmpty()) {
            int w = font.width(row.extra());
            context.text(font, Component.literal(row.extra()), x + width - w - 4, y, 0xFFAAAAAA);
        }
    }

    @Override
    protected boolean rowClicked(int index) {
        openProfile(rows.get(index).name());
        return true;
    }
}
