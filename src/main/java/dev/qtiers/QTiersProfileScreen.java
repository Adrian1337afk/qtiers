package dev.qtiers;

import dev.qtiers.TierProfile.Ranking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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

    public QTiersProfileScreen(String playerName) {
        super(Text.literal(playerName));
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
        int y = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("NameMC"),
                        b -> ConfirmLinkScreen.open(this, "https://namemc.com/profile/" + playerId, true))
                .dimensions(width / 2 - 154, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 - 50, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Settings"),
                        b -> client.setScreen(new QTiersConfigScreen(this)))
                .dimensions(width / 2 + 54, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(displayName).formatted(Formatting.BOLD), width / 2, 12, 0xFFFFFFFF);

        int top = 32;
        int bottom = height - 36;
        int skinPanel = Math.max(90, width / 4);
        renderSkin(context, 10, top, skinPanel - 10, bottom - top);

        // Sites in a 2-column grid (4 sites = 2x2), each panel getting an equal share of the height
        TierSource[] sources = TierSource.values();
        int gridX = skinPanel + 10;
        int cols = 2;
        int rows = (sources.length + cols - 1) / cols;
        int cellW = (width - gridX - 10) / cols;
        int cellH = (bottom - top) / rows;
        for (int i = 0; i < sources.length; i++) {
            renderPanel(context, sources[i], gridX + (i % cols) * cellW, top + (i / cols) * cellH, cellW, cellH - 4);
        }
    }

    private void renderSkin(DrawContext context, int x, int y, int w, int h) {
        int centerX = x + w / 2;
        switch (skinState) {
            case LOADING -> context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Loading skin..."), centerX, y + h / 2, 0xFFAAAAAA);
            case FAILED -> context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Skin unavailable"), centerX, y + h / 2, 0xFFFF5555);
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
        context.fill(x + 2, y - 2, x + w - 2, y + 22, 0x40000000 | (source.color & 0xFFFFFF));
        context.drawTextWithShadow(textRenderer,
                Text.literal(source.displayName).styled(s -> s.withColor(source.color).withBold(true)), x + 6, y + 1, 0xFFFFFFFF);

        if (failedSites.contains(source)) {
            context.drawTextWithShadow(textRenderer, Text.literal("Couldn't load"), x + 6, y + 12, 0xFFFF7777);
            return;
        }
        Optional<TierProfile> profile = profiles.get(source);
        if (profile == null) {
            context.drawTextWithShadow(textRenderer, Text.literal("Loading..."), x + 6, y + 12, 0xFFAAAAAA);
            return;
        }
        if (profile.isEmpty() || !profile.get().isRanked()) {
            context.drawTextWithShadow(textRenderer, Text.literal("Not ranked"), x + 6, y + 12, 0xFF888888);
            return;
        }
        TierProfile p = profile.get();
        String stats = p.summary();
        // Region is the least important part; drop it rather than run into the next panel
        if (!p.region().isEmpty() && textRenderer.getWidth(stats + " · " + p.region()) <= w - 10) stats += " · " + p.region();
        context.drawTextWithShadow(textRenderer, Text.literal(stats), x + 6, y + 12, 0xFFAAAAAA);

        int rowY = y + 28;
        List<Ranking> rankings = p.rankings().values().stream().sorted(Comparator.comparingInt(Ranking::score)).toList();
        int fits = Math.max(1, (y + h - rowY) / ROW_H);
        if (rankings.size() > fits) {
            // Keep the best ones and say how many are hidden
            context.drawTextWithShadow(textRenderer, Text.literal("+" + (rankings.size() - fits + 1) + " more"),
                    x + 6, rowY + (fits - 1) * ROW_H, 0xFF888888);
            rankings = rankings.subList(0, fits - 1);
        }
        for (Ranking r : rankings) {
            GameModes.Mode mode = GameModes.get(r.mode());
            MutableText line = Text.empty().append(GameModes.icon(source, r.mode())).append(" ")
                    .append(Text.literal(r.label()).styled(s -> s.withColor(r.color())))
                    .append(Text.literal(" " + mode.title()).styled(s -> s.withColor(0xD0D0D0)));
            if (r.hasHigherPeak()) {
                MutableText withPeak = line.copy()
                        .append(Text.literal(" ↑").formatted(Formatting.DARK_GRAY))
                        .append(Text.literal(Ranking.tierLabel(r.peakTier(), r.peakPos()))
                                .styled(s -> s.withColor(Ranking.tierColor(r.peakTier(), r.peakPos()))));
                // Drop the peak rather than overlap the next column on narrow screens
                if (textRenderer.getWidth(withPeak) <= w - 10) line = withPeak;
            }
            context.drawTextWithShadow(textRenderer, line, x + 6, rowY, 0xFFFFFFFF);
            rowY += ROW_H;
        }
    }

    /** Not removed(): that also fires when NameMC confirm / Settings open on top of this screen. */
    @Override
    public void close() {
        closed = true;
        if (skinState == SkinState.LOADED) client.getTextureManager().destroyTexture(skinId);
        super.close();
    }
}
