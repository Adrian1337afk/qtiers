package dev.qtiers;

import dev.qtiers.TierProfile.Ranking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** /qtiers <player>: full-body skin render plus every tier from each site. */
public class QTiersProfileScreen extends Screen {
    /**
     * Full-body renders, tried in order (same services the Tiers mod uses). Visage goes first:
     * mc-heads sometimes serves a stale default skin with a 200, which can't be detected.
     */
    private static final List<String> SKIN_RENDERERS = List.of(
            "https://visage.surgeplay.com/full/384/%s",
            "https://mc-heads.net/body/%s/256");
    private static final AtomicInteger TEXTURE_COUNTER = new AtomicInteger();
    private static final int SEARCH_W = 110;

    private enum SkinState { LOADING, LOADED, FAILED }

    private final String query;
    private final Identifier skinId = Identifier.of(QTiers.MOD_ID, "skin/" + TEXTURE_COUNTER.incrementAndGet());

    private volatile String displayName;
    private volatile String playerId; // uuid when known, otherwise the name
    /** Filled per site as each lookup finishes; a site in {@code failedSites} couldn't be reached. */
    private final Map<TierSource, Optional<TierProfile>> profiles = new ConcurrentHashMap<>();
    private final Set<TierSource> failedSites = ConcurrentHashMap.newKeySet();
    private volatile SkinState skinState = SkinState.LOADING;
    private int skinWidth;
    private int skinHeight;
    private boolean closed;
    private TextFieldWidget search;

    /** Screen to go back to on Done (the online players list or leaderboard); null closes to the game. */
    private final Screen parent;

    public QTiersProfileScreen(String playerName) {
        this(playerName, null);
    }

    public QTiersProfileScreen(String playerName, Screen parent) {
        super(Text.literal(playerName));
        this.parent = parent;
        this.query = playerName;
        this.displayName = playerName;
        this.playerId = playerName;
        load();
    }

    private void load() {
        MinecraftClient client = MinecraftClient.getInstance();
        TierApi.resolvePlayer(query).thenAccept(resolved -> {
            resolved.ifPresent(r -> {
                playerId = r[0];
                displayName = r[1];
            });
            loadSkin(0);
        });
        TierApi.lookupEach(query).forEach((source, future) -> future.whenComplete((result, err) -> client.execute(() -> {
            if (err != null) {
                failedSites.add(source);
                return;
            }
            profiles.put(source, result);
            // Use the site's capitalisation if Mojang didn't resolve the name
            if (displayName.equals(query)) {
                result.map(TierProfile::name).filter(n -> !n.isBlank()).ifPresent(n -> displayName = n);
            }
        })));
    }

    private void loadSkin(int rendererIndex) {
        if (rendererIndex >= SKIN_RENDERERS.size()) {
            skinState = SkinState.FAILED;
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        TierApi.fetchBytes(SKIN_RENDERERS.get(rendererIndex).formatted(java.net.URLEncoder.encode(playerId, java.nio.charset.StandardCharsets.UTF_8))).whenComplete((bytes, err) -> {
            if (err != null) {
                loadSkin(rendererIndex + 1);
                return;
            }
            client.execute(() -> {
                if (closed) return;
                try {
                    NativeImage image = NativeImage.read(bytes);
                    skinWidth = image.getWidth();
                    skinHeight = image.getHeight();
                    client.getTextureManager().registerTexture(skinId, new NativeImageBackedTexture(() -> "qtiers skin", image));
                    skinState = SkinState.LOADED;
                } catch (Exception e) {
                    QTiers.LOGGER.warn("Could not decode skin render for {}", query, e);
                    loadSkin(rendererIndex + 1);
                }
            });
        });
    }

    @Override
    protected void init() {
        int y = height - 32;
        // Bottom-left search box: type a name, press Enter to look that player up
        search = addDrawableChild(Ui.searchField(textRenderer, 12, y, SEARCH_W, search));

        // Buttons stay centred unless that would cover the search box; then they shrink to fit beside it
        int buttonW = 100;
        int startX = width / 2 - 154;
        int minX = 12 + SEARCH_W + 6;
        if (startX < minX) {
            buttonW = Math.min(100, (width - minX - 10 - 8) / 3);
            startX = minX;
        }
        addDrawableChild(Ui.button(startX, y, buttonW, 20, "NameMC",
                b -> ConfirmLinkScreen.open(this, "https://namemc.com/profile/" + java.net.URLEncoder.encode(playerId, java.nio.charset.StandardCharsets.UTF_8), true)));
        addDrawableChild(Ui.primary(startX + buttonW + 4, y, buttonW, 20, Text.translatable("gui.done"), b -> close()));
        addDrawableChild(Ui.button(startX + 2 * (buttonW + 4), y, buttonW, 20, "Settings",
                b -> client.setScreen(new QTiersConfigScreen(this))));
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (searchEntered(search, input)) {
            String name = search.getText();
            Screen back = parent;
            close();
            client.setScreen(new QTiersProfileScreen(name, back));
            return true;
        }
        return super.keyPressed(input);
    }

    /** True when Enter was pressed in the search box with a name long enough to look up. */
    static boolean searchEntered(TextFieldWidget field, KeyInput input) {
        return input.isEnter() && field != null && field.isFocused() && field.getText().length() >= 3;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        Ui.window(context, 4, 4, width - 8, height - 8, Text.literal(displayName));
        Ui.searchBox(context, search, 12, height - 32, SEARCH_W);
        super.render(context, mouseX, mouseY, deltaTicks);

        // Like the Tiers mod: skin on top in the middle, the sites side by side below it
        int top = 26;
        int bottom = height - 40;
        int skinH = Math.max(40, (bottom - top) / 4);
        renderSkin(context, width / 2 - skinH / 2, top, skinH, skinH);

        TierSource[] sources = TierSource.values();
        int cols = Math.min(3, sources.length);
        int rows = (sources.length + cols - 1) / cols;
        int gridY = top + skinH + 6;
        int cellW = (width - 24) / cols;
        int cellH = (bottom - gridY) / rows;
        for (int i = 0; i < sources.length; i++) {
            renderPanel(context, sources[i], 12 + (i % cols) * cellW, gridY + (i / cols) * cellH, cellW, cellH - 4, mouseX, mouseY);
        }
    }

    private void renderSkin(DrawContext context, int x, int y, int w, int h) {
        int centerX = x + w / 2;
        switch (skinState) {
            case LOADING -> Ui.centered(context, Text.literal("Loading skin..."), centerX, y + h / 2, Theme.palette().textDim());
            case FAILED -> Ui.centered(context, Text.literal("Skin unavailable"), centerX, y + h / 2, 0xFFEF4444);
            case LOADED -> {
                float scale = Math.min((float) w / skinWidth, (float) h / skinHeight);
                int drawW = Math.round(skinWidth * scale);
                int drawH = Math.round(skinHeight * scale);
                context.drawTexture(RenderPipelines.GUI_TEXTURED, skinId,
                        centerX - drawW / 2, y + (h - drawH) / 2, 0, 0, drawW, drawH, drawW, drawH);
            }
        }
    }

    /** One site: name, summary line, then a grid of mode icons with the tier (and higher peak) under each. */
    private void renderPanel(DrawContext context, TierSource source, int x, int y, int w, int h, int mouseX, int mouseY) {
        Theme.Palette pal = Theme.palette();
        int cx = x + w / 2;
        Ui.rect(context, x + 2, y, w - 4, h, Theme.radius(), pal.card());
        Ui.centered(context, Text.literal(source.displayName).styled(s -> s.withColor(source.color).withBold(true)), cx, y + 4, pal.text());

        Text status = null;
        int statusColor = pal.textMuted();
        Optional<TierProfile> profile = profiles.get(source);
        if (failedSites.contains(source)) {
            status = Text.literal("Couldn't load");
            statusColor = 0xFFEF4444;
        } else if (profile == null) {
            status = Text.literal("Loading...");
            statusColor = pal.textDim();
        } else if (profile.isEmpty() || !profile.get().isRanked()) {
            status = Text.literal("Not ranked");
        }
        if (status != null) {
            Ui.centered(context, status, cx, y + 16, statusColor);
            return;
        }

        TierProfile p = profile.get();
        String stats = p.summary();
        // Narrow panels: drop trailing parts like the points total rather than overflow
        while (textRenderer.getWidth(stats) > w - 10 && stats.contains(" · ")) stats = stats.substring(0, stats.lastIndexOf(" · "));
        if (!p.region().isEmpty() && textRenderer.getWidth(stats + " · " + p.region()) <= w - 10) stats += " · " + p.region();
        Ui.centered(context, Text.literal(stats), cx, y + 16, pal.textDim());

        List<Ranking> rankings = p.rankings().values().stream().sorted(Comparator.comparingInt(Ranking::score)).toList();
        int slotW = 24;
        int perRow = Math.max(1, Math.min(rankings.size(), (w - 8) / slotW));
        int gridTop = y + 28;
        int rowsNeeded = (rankings.size() + perRow - 1) / perRow;
        // Big icons only when every ranking still fits with them
        int iconScale = rowsNeeded * 28 <= y + h - gridTop ? 2 : 1;
        int slotH = 9 * iconScale + 10;
        int slots = perRow * Math.max(1, (y + h - gridTop) / slotH);
        int hidden = 0;
        if (rankings.size() > slots) {
            // Keep the best; the last slot says how many are left out
            hidden = rankings.size() - (slots - 1);
            rankings = rankings.subList(0, slots - 1);
        }
        int total = rankings.size() + (hidden > 0 ? 1 : 0);

        Text hovered = null;
        for (int i = 0; i < total; i++) {
            int rowCount = Math.min(perRow, total - (i / perRow) * perRow);
            int sx = cx - rowCount * slotW / 2 + (i % perRow) * slotW;
            int sy = gridTop + (i / perRow) * slotH;
            int scx = sx + slotW / 2;
            int labelY = sy + 9 * iconScale + 1;
            if (i == rankings.size()) {
                Ui.centered(context, Text.literal("+" + hidden), scx, labelY, pal.textMuted());
                break;
            }
            Ranking r = rankings.get(i);

            Text icon = ModeTexts.icon(source, r.mode());
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(scx - textRenderer.getWidth(icon) * iconScale / 2f, sy);
            context.getMatrices().scale(iconScale, iconScale);
            context.drawText(textRenderer, icon, 0, 0, 0xFFFFFFFF, false);
            context.getMatrices().popMatrix();

            Text label = Text.literal(r.label()).styled(s -> s.withColor(r.color()));
            // A higher peak gets an arrow here and the full peak in the tooltip
            if (r.hasHigherPeak()) label = label.copy().append(Text.literal("↑").formatted(Formatting.DARK_GRAY));
            Ui.centered(context, label, scx, labelY, pal.text());
            if (mouseX >= sx && mouseX < sx + slotW && mouseY >= sy && mouseY < sy + slotH) {
                hovered = Text.literal(GameModes.get(r.mode()).title() + (r.hasHigherPeak()
                        ? " · peak " + Ranking.tierLabel(r.peakTier(), r.peakPos()) : ""));
            }
        }
        if (hovered != null) context.drawTooltip(textRenderer, hovered, mouseX, mouseY);
    }

    /** Not removed(): that also fires when NameMC confirm / Settings open on top of this screen. */
    @Override
    public void close() {
        closed = true;
        if (skinState == SkinState.LOADED) client.getTextureManager().destroyTexture(skinId);
        client.setScreen(parent);
    }
}
