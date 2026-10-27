package dev.qtiers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** /qtiers top: the top players of a site, overall or for one gamemode. */
public class QTiersLeaderboardScreen extends QTiersListScreen {
    private TierSource site;
    private String mode;
    private List<Leaderboards.Row> rows = List.of();
    private Text status = Text.literal("Loading...");
    /** Bumped on every new request so a slow, outdated answer can't replace a newer one. */
    private int request;

    public QTiersLeaderboardScreen(@Nullable Screen parent) {
        this(parent, Leaderboards.SITES.getFirst(), Leaderboards.OVERALL);
    }

    public QTiersLeaderboardScreen(@Nullable Screen parent, TierSource site, String mode) {
        super(Text.literal("Leaderboards"), parent);
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
        addDrawableChild(CyclingButtonWidget.<TierSource>builder(
                        s -> Text.literal(s.displayName).styled(st -> st.withColor(s.color)), site)
                .values(Leaderboards.SITES)
                .build(left, 24, 150, 20, Text.literal("Site"), (b, v) -> {
                    site = v;
                    mode = Leaderboards.OVERALL;
                    clearAndInit();
                    load();
                }));
        addDrawableChild(CyclingButtonWidget.<String>builder(this::modeName, mode)
                .values(Leaderboards.modesFor(site))
                .build(left + 154, 24, 150, 20, Text.literal("Mode"), (b, v) -> {
                    mode = v;
                    load();
                }));
        if (rows.isEmpty() && request == 0) load();
    }

    private Text modeName(String m) {
        return Leaderboards.OVERALL.equals(m) ? Text.literal("Overall").styled(s -> s.withColor(0xFFD166))
                : ModeTexts.styledName(site, m);
    }

    private void load() {
        int id = ++request;
        rows = List.of();
        status = Text.literal("Loading...");
        Leaderboards.fetch(site, mode).whenComplete((result, err) -> client.execute(() -> {
            if (id != request) return;
            if (err != null) {
                QTiers.LOGGER.warn("Could not load {} leaderboard: {}", site.displayName, err.toString());
                status = Text.literal("Couldn't load " + site.displayName).styled(s -> s.withColor(0xFF7777));
            } else {
                rows = result;
                status = Text.literal("Nobody ranked yet");
            }
        }));
    }

    @Override
    protected int rowCount() {
        return rows.size();
    }

    @Override
    protected Text emptyText() {
        return status;
    }

    @Override
    protected void renderRow(DrawContext context, int index, int x, int y, int width, boolean hovered) {
        Leaderboards.Row row = rows.get(index);
        boolean overall = Leaderboards.OVERALL.equals(mode);
        context.drawTextWithShadow(textRenderer, Text.literal((index + 1) + "."), x, y, 0xFF888888);
        int nameX = x + 28;
        if (!overall) {
            // Gamemode boards: tier label in front of the name, like a tier list
            context.drawTextWithShadow(textRenderer, Text.empty()
                    .append(ModeTexts.icon(site, mode))
                    .append(Text.literal(row.label()).styled(s -> s.withColor(row.color()))), nameX, y, 0xFFFFFFFF);
            nameX += 44;
        }
        drawClipped(context, Text.literal(row.name()), nameX, y, width - (nameX - x) - 60, hovered ? 0xFFFFFF55 : 0xFFFFFFFF);
        if (!row.extra().isEmpty()) {
            int w = textRenderer.getWidth(row.extra());
            context.drawTextWithShadow(textRenderer, Text.literal(row.extra()), x + width - w - 4, y, 0xFFAAAAAA);
        }
    }

    @Override
    protected boolean rowClicked(int index) {
        openProfile(rows.get(index).name());
        return true;
    }
}
