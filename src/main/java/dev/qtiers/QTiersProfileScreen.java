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
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** /qtiers <player>: full-body skin render plus every tier from all three sites. */
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
    private volatile Map<TierSource, Optional<TierProfile>> profiles;
    private volatile boolean lookupFailed;
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
        TierApi.lookupAll(query).whenComplete((result, err) -> client.execute(() -> {
            if (err != null) {
                lookupFailed = true;
                return;
            }
            profiles = result;
            if (displayName.equals(query)) {
                result.values().stream().filter(java.util.Objects::nonNull).flatMap(Optional::stream).map(TierProfile::name)
                        .filter(n -> !n.isBlank()).findFirst().ifPresent(n -> displayName = n);
            }
        }));
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

        int colX = skinPanel + 10;
        int colW = (width - colX - 10) / TierSource.values().length;
        if (lookupFailed) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Could not reach the tier sites"),
                    colX + (width - colX) / 2, top + 20, 0xFFFF5555);
            return;
        }
        if (profiles == null) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Loading tiers..."),
                    colX + (width - colX) / 2, top + 20, 0xFFAAAAAA);
            return;
        }
        for (TierSource source : TierSource.values()) {
            renderColumn(context, source, profiles.get(source), colX, top, colW);
            colX += colW;
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

    /** {@code profile} is null when the site couldn't be reached (see TierApi.lookupAll). */
    private void renderColumn(DrawContext context, TierSource source, @Nullable Optional<TierProfile> profile, int x, int y, int w) {
        context.fill(x + 2, y - 2, x + w - 2, y + 22, 0x40000000 | (source.color & 0xFFFFFF));
        context.drawTextWithShadow(textRenderer,
                Text.literal(source.displayName).styled(s -> s.withColor(source.color).withBold(true)), x + 6, y + 1, 0xFFFFFFFF);

        if (profile == null) {
            context.drawTextWithShadow(textRenderer, Text.literal("Couldn't load"), x + 6, y + 12, 0xFFFF7777);
            return;
        }
        if (profile.isEmpty() || !profile.get().isRanked()) {
            context.drawTextWithShadow(textRenderer, Text.literal("Not ranked"), x + 6, y + 12, 0xFF888888);
            return;
        }
        TierProfile p = profile.get();
        String region = p.region().isBlank() || p.region().equals("??") ? "" : " · " + p.region();
        String stats = "#" + p.overall() + " · " + p.points() + " pts";
        // Region is the least important part; drop it rather than run into the next column
        if (textRenderer.getWidth(stats + region) <= w - 10) stats += region;
        context.drawTextWithShadow(textRenderer, Text.literal(stats), x + 6, y + 12, 0xFFAAAAAA);

        int rowY = y + 28;
        List<Ranking> rankings = p.rankings().values().stream().sorted(Comparator.comparingInt(Ranking::score)).toList();
        for (Ranking r : rankings) {
            GameModes.Mode mode = GameModes.get(r.mode());
            MutableText line = Text.empty().append(GameModes.icon(r.mode())).append(" ")
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
