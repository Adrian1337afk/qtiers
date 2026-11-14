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
import net.minecraft.text.MutableText;
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
    private static final int ROW_H = 11;
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
        TierApi.fetchBytes(SKIN_RENDERERS.get(rendererIndex).formatted(playerId)).whenComplete((bytes, err) -> {
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
                b -> ConfirmLinkScreen.open(this, "https://namemc.com/profile/" + playerId, true)));
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
        Theme.Palette pal = Theme.palette();
        Ui.window(context, 4, 4, width - 8, height - 8, Text.literal(displayName));
        Ui.searchBox(context, search, 12, height - 32, SEARCH_W);
        super.render(context, mouseX, mouseY, deltaTicks);

        int top = 32;
        int bottom = height - 40;
        int skinPanel = Math.max(90, width / 4);
        Ui.rect(context, 12, top - 2, skinPanel - 14, bottom - top + 2, Theme.radius(), pal.card());
        renderSkin(context, 12, top + 4, skinPanel - 14, bottom - top - 8);

        // Sites in a grid (4 sites = 2x2, 5+ = 3 columns), each panel getting an equal share of the height
        TierSource[] sources = TierSource.values();
        int gridX = skinPanel + 10;
        int cols = sources.length > 4 ? 3 : 2;
        int rows = (sources.length + cols - 1) / cols;
        int cellW = (width - gridX - 12) / cols;
        int cellH = (bottom - top) / rows;
        for (int i = 0; i < sources.length; i++) {
            renderPanel(context, sources[i], gridX + (i % cols) * cellW, top + (i / cols) * cellH, cellW, cellH - 4);
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

    /** One site's panel: header, stats line, then as many rankings as fit in {@code h}. */
    private void renderPanel(DrawContext context, TierSource source, int x, int y, int w, int h) {
        Theme.Palette pal = Theme.palette();
        Ui.rect(context, x + 2, y - 2, w - 4, h + 2, Theme.radius(), pal.card());
        Ui.rect(context, x + 2, y - 2, w - 4, 24, Theme.radius(), Theme.alpha(source.color, 0x30));
        Ui.rect(context, x + 2, y + 2, 3, 16, 1, 0xFF000000 | source.color);
        Ui.text(context, Text.literal(source.displayName).styled(s -> s.withColor(source.color).withBold(true)), x + 9, y + 1, pal.text());

        if (failedSites.contains(source)) {
            Ui.text(context, Text.literal("Couldn't load"), x + 9, y + 12, 0xFFEF4444);
            return;
        }
        Optional<TierProfile> profile = profiles.get(source);
        if (profile == null) {
            Ui.text(context, Text.literal("Loading..."), x + 9, y + 12, pal.textDim());
            return;
        }
        if (profile.isEmpty() || !profile.get().isRanked()) {
            Ui.text(context, Text.literal("Not ranked"), x + 9, y + 12, pal.textMuted());
            return;
        }
        TierProfile p = profile.get();
        String stats = p.summary();
        // Narrow panels (3 columns): drop trailing parts like the points total rather than overflow
        while (textRenderer.getWidth(stats) > w - 10 && stats.contains(" · ")) stats = stats.substring(0, stats.lastIndexOf(" · "));
        // Region is the least important part; drop it rather than run into the next panel
        if (!p.region().isEmpty() && textRenderer.getWidth(stats + " · " + p.region()) <= w - 10) stats += " · " + p.region();
        Ui.text(context, Text.literal(stats), x + 9, y + 12, pal.textDim());

        int rowY = y + 28;
        List<Ranking> rankings = p.rankings().values().stream().sorted(Comparator.comparingInt(Ranking::score)).toList();
        int fits = Math.max(1, (y + h - rowY) / ROW_H);
        if (rankings.size() > fits) {
            // Keep the best ones and say how many are hidden
            Ui.text(context, Text.literal("+" + (rankings.size() - fits + 1) + " more"),
                    x + 9, rowY + (fits - 1) * ROW_H, pal.textMuted());
            rankings = rankings.subList(0, fits - 1);
        }
        for (Ranking r : rankings) {
            GameModes.Mode mode = GameModes.get(r.mode());
            MutableText line = Text.empty().append(ModeTexts.icon(source, r.mode())).append(" ")
                    .append(Text.literal(r.label()).styled(s -> s.withColor(r.color())))
                    .append(Text.literal(" " + mode.title()).styled(s -> s.withColor(pal.textDim() & 0xFFFFFF)));
            if (r.hasHigherPeak()) {
                MutableText withPeak = line.copy()
                        .append(Text.literal(" ↑").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal(Ranking.tierLabel(r.peakTier(), r.peakPos()))
                                .styled(s -> s.withColor(Ranking.tierColor(r.peakTier(), r.peakPos()))));
                // Drop the peak rather than overlap the next column on narrow screens
                if (textRenderer.getWidth(withPeak) <= w - 10) line = withPeak;
            }
            Ui.text(context, line, x + 9, rowY, pal.text());
            rowY += ROW_H;
        }
    }

    /** Not removed(): that also fires when NameMC confirm / Settings open on top of this screen. */
    @Override
    public void close() {
        closed = true;
        if (skinState == SkinState.LOADED) client.getTextureManager().destroyTexture(skinId);
        client.setScreen(parent);
    }
}
