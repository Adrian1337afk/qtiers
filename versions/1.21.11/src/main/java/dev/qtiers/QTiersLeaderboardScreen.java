package dev.qtiers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
        return 64;
    }

    @Override
    protected void init() {
        super.init();
        int left = width / 2 - 152;
        addDrawableChild(Ui.button(left, 26, 150, 20, "", b -> {
            List<TierSource> sites = Leaderboards.SITES;
            site = sites.get((sites.indexOf(site) + 1) % sites.size());
            mode = Leaderboards.OVERALL;
            clearAndInit();
            load();
        })).setMessage(Text.literal("Site: ").append(Text.literal(site.displayName).styled(st -> st.withColor(site.color))));
        addDrawableChild(Ui.button(left + 154, 26, 150, 20, "", b -> {
            List<String> modes = Leaderboards.modesFor(site);
            mode = modes.get((modes.indexOf(mode) + 1) % modes.size());
            b.setMessage(Text.literal("Mode: ").append(modeName(mode)));
            load();
        })).setMessage(Text.literal("Mode: ").append(modeName(mode)));
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
        Ui.text(context, Text.literal((index + 1) + "."), x, y, mutedColor());
        int nameX = x + 28;
        if (!overall) {
            // Gamemode boards: tier label in front of the name, like a tier list
            Ui.text(context, Text.empty()
                    .append(ModeTexts.icon(site, mode))
                    .append(Text.literal(row.label()).styled(s -> s.withColor(row.color()))), nameX, y, textColor());
            nameX += 44;
        }
        drawClipped(context, Text.literal(row.name()), nameX, y, width - (nameX - x) - 60, hovered ? hoverColor() : textColor());
        if (!row.extra().isEmpty()) {
            int w = textRenderer.getWidth(row.extra());
            Ui.text(context, Text.literal(row.extra()), x + width - w - 4, y, dimColor());
        }
    }

    @Override
    protected boolean rowClicked(int index) {
        openProfile(rows.get(index).name());
        return true;
    }
}
