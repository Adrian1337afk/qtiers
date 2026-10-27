package dev.qtiers;

import dev.qtiers.TierProfile.Ranking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

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
    private final Identifier skinId = Identifier.fromNamespaceAndPath(QTiers.MOD_ID, "skin/" + TEXTURE_COUNTER.incrementAndGet());

    private volatile String displayName;
    private volatile String playerId; // uuid when known, otherwise the name
    /** Filled per site as each lookup finishes; a site in {@code failedSites} couldn't be reached. */
    private final Map<TierSource, Optional<TierProfile>> profiles = new ConcurrentHashMap<>();
    private final Set<TierSource> failedSites = ConcurrentHashMap.newKeySet();
    private volatile SkinState skinState = SkinState.LOADING;
    private int skinWidth;
    private int skinHeight;
    private boolean closed;
    private EditBox search;

    /** Screen to go back to on Done (the online players list or leaderboard); null closes to the game. */
    private final Screen parent;

    public QTiersProfileScreen(String playerName) {
        this(playerName, null);
    }

    public QTiersProfileScreen(String playerName, Screen parent) {
        super(Component.literal(playerName));
        this.parent = parent;
        this.query = playerName;
        this.displayName = playerName;
        this.playerId = playerName;
        load();
    }

    private void load() {
        Minecraft client = Minecraft.getInstance();
        TierApi.resolvePlayer(query).thenAccept(resolved -> {
            resolved.ifPresent(r -> {
                playerId = r[0];
                displayName = r[1];
            });
            loadSkin(0);
        });
        TierApi.lookupEach(query).forEach((source, future) -> future.whenComplete((result, err) -> minecraft.execute(() -> {
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
        Minecraft client = Minecraft.getInstance();
        TierApi.fetchBytes(SKIN_RENDERERS.get(rendererIndex).formatted(playerId)).whenComplete((bytes, err) -> {
            if (err != null) {
                loadSkin(rendererIndex + 1);
                return;
            }
            minecraft.execute(() -> {
                if (closed) return;
                try {
                    NativeImage image = NativeImage.read(bytes);
                    skinWidth = image.getWidth();
                    skinHeight = image.getHeight();
                    minecraft.getTextureManager().register(skinId, new DynamicTexture(() -> "qtiers skin", image));
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
        // Bottom-left search box: type a name, press Enter to look that player up
        search = addRenderableWidget(searchField(font, 10, y, SEARCH_W, search));

        // Buttons stay centred unless that would cover the search box; then they shrink to fit beside it
        int buttonW = 100;
        int startX = width / 2 - 154;
        int minX = 10 + SEARCH_W + 6;
        if (startX < minX) {
            buttonW = Math.min(100, (width - minX - 10 - 8) / 3);
            startX = minX;
        }
        addRenderableWidget(Button.builder(Component.literal("NameMC"),
                        b -> ConfirmLinkScreen.confirmLinkNow(this, "https://namemc.com/profile/" + playerId, true))
                .bounds(startX, y, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(startX + buttonW + 4, y, buttonW, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Settings"),
                        b -> minecraft.gui.setScreen(new QTiersConfigScreen(this)))
                .bounds(startX + 2 * (buttonW + 4), y, buttonW, 20).build());
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (searchEntered(search, input)) {
            String name = search.getValue();
            Screen back = parent;
            onClose();
            minecraft.gui.setScreen(new QTiersProfileScreen(name, back));
            return true;
        }
        return super.keyPressed(input);
    }

    /** The "Search player..." box, shared with the settings screen. Keeps typed text across resizes. */
    static EditBox searchField(Font font, int x, int y, int width, EditBox previous) {
        EditBox field = new EditBox(font, x, y, width, 20, Component.literal("Search player"));
        field.setMaxLength(16);
        field.setHint(Component.literal("Search player...").withStyle(ChatFormatting.DARK_GRAY));
        if (previous != null) field.setValue(previous.getValue());
        return field;
    }

    /** True when Enter was pressed in the search box with a name long enough to look up. */
    static boolean searchEntered(EditBox field, KeyEvent input) {
        return input.isConfirmation() && field != null && field.isFocused() && field.getValue().matches("[A-Za-z0-9_]{3,16}");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);

        context.centeredText(font,
                Component.literal(displayName).withStyle(ChatFormatting.BOLD), width / 2, 12, 0xFFFFFFFF);

        int top = 32;
        int bottom = height - 36;
        int skinPanel = Math.max(90, width / 4);
        renderSkin(context, 10, top, skinPanel - 10, bottom - top);

        // Sites in a grid (4 sites = 2x2, 5+ = 3 columns), each panel getting an equal share of the height
        TierSource[] sources = TierSource.values();
        int gridX = skinPanel + 10;
        int cols = sources.length > 4 ? 3 : 2;
        int rows = (sources.length + cols - 1) / cols;
        int cellW = (width - gridX - 10) / cols;
        int cellH = (bottom - top) / rows;
        for (int i = 0; i < sources.length; i++) {
            renderPanel(context, sources[i], gridX + (i % cols) * cellW, top + (i / cols) * cellH, cellW, cellH - 4);
        }
    }

    private void renderSkin(GuiGraphicsExtractor context, int x, int y, int w, int h) {
        int centerX = x + w / 2;
        switch (skinState) {
            case LOADING -> context.centeredText(font,
                    Component.literal("Loading skin..."), centerX, y + h / 2, 0xFFAAAAAA);
            case FAILED -> context.centeredText(font,
                    Component.literal("Skin unavailable"), centerX, y + h / 2, 0xFFFF5555);
            case LOADED -> {
                float scale = Math.min((float) w / skinWidth, (float) h / skinHeight);
                int drawW = Math.round(skinWidth * scale);
                int drawH = Math.round(skinHeight * scale);
                context.blit(RenderPipelines.GUI_TEXTURED, skinId,
                        centerX - drawW / 2, y + (h - drawH) / 2, 0, 0, drawW, drawH, drawW, drawH);
            }
        }
    }

    /** One site's panel: header, stats line, then as many rankings as fit in {@code h}. */
    private void renderPanel(GuiGraphicsExtractor context, TierSource source, int x, int y, int w, int h) {
        context.fill(x + 2, y - 2, x + w - 2, y + 22, 0x40000000 | (source.color & 0xFFFFFF));
        context.text(font,
                Component.literal(source.displayName).withStyle(s -> s.withColor(source.color).withBold(true)), x + 6, y + 1, 0xFFFFFFFF);

        if (failedSites.contains(source)) {
            context.text(font, Component.literal("Couldn't load"), x + 6, y + 12, 0xFFFF7777);
            return;
        }
        Optional<TierProfile> profile = profiles.get(source);
        if (profile == null) {
            context.text(font, Component.literal("Loading..."), x + 6, y + 12, 0xFFAAAAAA);
            return;
        }
        if (profile.isEmpty() || !profile.get().isRanked()) {
            context.text(font, Component.literal("Not ranked"), x + 6, y + 12, 0xFF888888);
            return;
        }
        TierProfile p = profile.get();
        String stats = p.summary();
        // Narrow panels (3 columns): drop trailing parts like the points total rather than overflow
        while (font.width(stats) > w - 10 && stats.contains(" · ")) stats = stats.substring(0, stats.lastIndexOf(" · "));
        // Region is the least important part; drop it rather than run into the next panel
        if (!p.region().isEmpty() && font.width(stats + " · " + p.region()) <= w - 10) stats += " · " + p.region();
        context.text(font, Component.literal(stats), x + 6, y + 12, 0xFFAAAAAA);

        int rowY = y + 28;
        List<Ranking> rankings = p.rankings().values().stream().sorted(Comparator.comparingInt(Ranking::score)).toList();
        int fits = Math.max(1, (y + h - rowY) / ROW_H);
        if (rankings.size() > fits) {
            // Keep the best ones and say how many are hidden
            context.text(font, Component.literal("+" + (rankings.size() - fits + 1) + " more"),
                    x + 6, rowY + (fits - 1) * ROW_H, 0xFF888888);
            rankings = rankings.subList(0, fits - 1);
        }
        for (Ranking r : rankings) {
            GameModes.Mode mode = GameModes.get(r.mode());
            MutableComponent line = Component.empty().append(ModeTexts.icon(source, r.mode())).append(" ")
                    .append(Component.literal(r.label()).withStyle(s -> s.withColor(r.color())))
                    .append(Component.literal(" " + mode.title()).withStyle(s -> s.withColor(0xD0D0D0)));
            if (r.hasHigherPeak()) {
                MutableComponent withPeak = line.copy()
                        .append(Component.literal(" ↑").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(Ranking.tierLabel(r.peakTier(), r.peakPos()))
                                .withStyle(s -> s.withColor(Ranking.tierColor(r.peakTier(), r.peakPos()))));
                // Drop the peak rather than overlap the next column on narrow screens
                if (font.width(withPeak) <= w - 10) line = withPeak;
            }
            context.text(font, line, x + 6, rowY, 0xFFFFFFFF);
            rowY += ROW_H;
        }
    }

    /** Not removed(): that also fires when NameMC confirm / Settings open on top of this screen. */
    @Override
    public void onClose() {
        closed = true;
        if (skinState == SkinState.LOADED) minecraft.getTextureManager().release(skinId);
        minecraft.gui.setScreen(parent);
    }
}
