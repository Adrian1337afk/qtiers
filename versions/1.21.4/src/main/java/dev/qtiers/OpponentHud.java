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
        targetName = player.getGameProfile().getName();
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
            line.append(Text.literal(r.label()).styled(s -> s.withColor(r.color())))
                    .append(Text.literal(" " + GameModes.get(r.mode()).title()).styled(s -> s.withColor(0xBBBBBB)));
            lines.add(line);
        }
        Text title = Text.literal(name).styled(s -> s.withBold(true));
        if (lines.isEmpty()) lines.add(Text.literal("No tiers").styled(s -> s.withColor(0x888888)));

        var tr = MinecraftClient.getInstance().textRenderer;
        int w = tr.getWidth(title);
        for (Text line : lines) w = Math.max(w, tr.getWidth(line));
        return new Box(title, lines, w + 8, 14 + lines.size() * 10);
    }

    /** Top-left corner of the box from the saved position, kept fully on screen. */
    static int[] position(Box box, int screenWidth, int screenHeight) {
        QTiersConfig config = QTiersConfig.get();
        int freeX = Math.max(0, screenWidth - box.width() - 2 * MARGIN);
        int freeY = Math.max(0, screenHeight - box.height() - 2 * MARGIN);
        int x = MARGIN + (int) Math.round(clamp(config.opponentHudX) * freeX);
        int y = MARGIN + (int) Math.round(clamp(config.opponentHudY) * freeY);
        return new int[]{x, y};
    }

    static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    static void draw(DrawContext context, Box box, int x, int y) {
        var tr = MinecraftClient.getInstance().textRenderer;
        context.fill(x, y, x + box.width(), y + box.height(), 0x90000000);
        context.drawTextWithShadow(tr, box.title(), x + 4, y + 3, 0xFFFFFFFF);
        int ly = y + 15;
        for (Text line : box.lines()) {
            context.drawTextWithShadow(tr, line, x + 4, ly, 0xFFFFFFFF);
            ly += 10;
        }
    }
}
