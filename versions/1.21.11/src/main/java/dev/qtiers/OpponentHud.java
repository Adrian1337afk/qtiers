package dev.qtiers;

import dev.qtiers.TierProfile.Ranking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Small box with the tiers of the last player you hit, shown for a few seconds after each hit.
 * Its position is set in {@link QTiersHudEditScreen} (default: top-left corner).
 */
public final class OpponentHud {
    private static final long SHOW_MS = 10_000;
    /** Gap kept between the box and the screen edge. */
    static final int MARGIN = 4;

    private static UUID target;
    private static String targetName;
    private static long lastHit;

    private OpponentHud() {}

    /** The box's lines and size, ready to draw anywhere. */
    record Box(Text title, List<Text> lines, int width, int height) {}

    public static void onAttack(PlayerEntity player) {
        target = player.getUuid();
        targetName = player.getGameProfile().name();
        lastHit = System.currentTimeMillis();
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        QTiersConfig config = QTiersConfig.get();
        MinecraftClient client = MinecraftClient.getInstance();
        if (!config.enabled || !config.opponentHud || target == null || client.options.hudHidden) return;
        if (System.currentTimeMillis() - lastHit > SHOW_MS) return;
        // The edit screen draws its own preview
        if (client.currentScreen instanceof QTiersHudEditScreen) return;

        Box box = build(target, targetName);
        int[] pos = position(box, context.getScaledWindowWidth(), context.getScaledWindowHeight());
        draw(context, box, pos[0], pos[1]);
    }

    /** Builds the box for a player from their cached tiers, using each site's chosen gamemode. */
    static Box build(UUID uuid, String name) {
        QTiersConfig config = QTiersConfig.get();
        List<Text> lines = new ArrayList<>();
        for (TierSource source : TierSource.values()) {
            Optional<Ranking> ranking = TierApi.getCached(uuid, source)
                    .flatMap(p -> TierDisplay.pick(p, config.site(source).gamemode, config));
            if (ranking.isEmpty()) continue;
            Ranking r = ranking.get();
            MutableText line = Text.literal(source.shortName + " ").styled(s -> s.withColor(source.color));
            if (config.showIcons) line.append(ModeTexts.icon(source, r.mode()));
            line.append(Text.literal(r.label()).styled(s -> s.withColor(r.color())));
            if (config.hudModeNames) {
                line.append(Text.literal(" " + GameModes.get(r.mode()).title()).styled(s -> s.withColor(darkText() ? 0x555555 : 0xBBBBBB)));
            }
            lines.add(line);
        }
        Text title = Text.literal(name).styled(s -> s.withBold(true));
        if (lines.isEmpty()) lines.add(Text.literal("No tiers").styled(s -> s.withColor(0x888888)));

        var tr = MinecraftClient.getInstance().textRenderer;
        int w = tr.getWidth(title);
        for (Text line : lines) w = Math.max(w, tr.getWidth(line));
        return new Box(title, lines, w + 8, 14 + lines.size() * 10);
    }

    static int scaledWidth(Box box) {
        return (int) Math.ceil(box.width() * QTiersConfig.get().hudScale);
    }

    static int scaledHeight(Box box) {
        return (int) Math.ceil(box.height() * QTiersConfig.get().hudScale);
    }

    /** True when the chosen background is light enough that the text has to be dark. */
    private static boolean darkText() {
        QTiersConfig config = QTiersConfig.get();
        return config.hudOpacity >= 50 && Theme.luminance(config.hudBackground) > 0.6;
    }

    /** Top-left corner of the box from the saved position, kept fully on screen. */
    static int[] position(Box box, int screenWidth, int screenHeight) {
        QTiersConfig config = QTiersConfig.get();
        int freeX = Math.max(0, screenWidth - scaledWidth(box) - 2 * MARGIN);
        int freeY = Math.max(0, screenHeight - scaledHeight(box) - 2 * MARGIN);
        int x = MARGIN + (int) Math.round(clamp(config.opponentHudX) * freeX);
        int y = MARGIN + (int) Math.round(clamp(config.opponentHudY) * freeY);
        return new int[]{x, y};
    }

    static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    /** Draws the box at x, y in the configured scale and style. */
    static void draw(DrawContext context, Box box, int x, int y) {
        QTiersConfig config = QTiersConfig.get();
        var tr = MinecraftClient.getInstance().textRenderer;
        float scale = (float) config.hudScale;
        var matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);

        int radius = Theme.radius();
        Ui.rect(context, 0, 0, box.width(), box.height(), radius, Theme.alpha(config.hudBackground, config.hudOpacity * 255 / 100));
        if (config.hudBorder) Ui.outline(context, 0, 0, box.width(), box.height(), radius, Theme.accent());
        int textColor = darkText() ? 0xFF111111 : 0xFFFFFFFF;
        int titleColor = config.hudAccentTitle ? Theme.accent() : textColor;
        context.drawText(tr, box.title(), 4, 3, titleColor, config.hudShadow);
        int ly = 15;
        for (Text line : box.lines()) {
            context.drawText(tr, line, 4, ly, textColor, config.hudShadow);
            ly += 10;
        }
        matrices.popMatrix();
    }
}
