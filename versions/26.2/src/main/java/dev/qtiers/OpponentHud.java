package dev.qtiers;

import dev.qtiers.TierProfile.Ranking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

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
    record Box(Component title, List<Component> lines, int width, int height) {}

    public static void onAttack(Player player) {
        target = player.getUUID();
        targetName = player.getGameProfile().name();
        lastHit = System.currentTimeMillis();
    }

    public static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        QTiersConfig config = QTiersConfig.get();
        Minecraft client = Minecraft.getInstance();
        if (!config.enabled || !config.opponentHud || target == null || client.gui.hud.isHidden()) return;
        if (System.currentTimeMillis() - lastHit > SHOW_MS) return;
        // The edit screen draws its own preview
        if (client.gui.screen() instanceof QTiersHudEditScreen) return;

        Box box = build(target, targetName);
        int[] pos = position(box, context.guiWidth(), context.guiHeight());
        draw(context, box, pos[0], pos[1]);
    }

    /** Builds the box for a player from their cached tiers, using each site's chosen gamemode. */
    static Box build(UUID uuid, String name) {
        QTiersConfig config = QTiersConfig.get();
        List<Component> lines = new ArrayList<>();
        for (TierSource source : TierSource.values()) {
            Optional<Ranking> ranking = TierApi.getCached(uuid, source)
                    .flatMap(p -> TierDisplay.pick(p, config.site(source).gamemode, config));
            if (ranking.isEmpty()) continue;
            Ranking r = ranking.get();
            MutableComponent line = Component.literal(source.shortName + " ").withStyle(s -> s.withColor(source.color));
            if (config.showIcons) line.append(ModeTexts.icon(source, r.mode()));
            line.append(Component.literal(r.label()).withStyle(s -> s.withColor(r.color())))
                    .append(Component.literal(" " + GameModes.get(r.mode()).title()).withStyle(s -> s.withColor(0xBBBBBB)));
            lines.add(line);
        }
        Component title = Component.literal(name).withStyle(s -> s.withBold(true));
        if (lines.isEmpty()) lines.add(Component.literal("No tiers").withStyle(s -> s.withColor(0x888888)));

        var tr = Minecraft.getInstance().font;
        int w = tr.width(title);
        for (Component line : lines) w = Math.max(w, tr.width(line));
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

    static void draw(GuiGraphicsExtractor context, Box box, int x, int y) {
        var tr = Minecraft.getInstance().font;
        context.fill(x, y, x + box.width(), y + box.height(), 0x90000000);
        context.text(tr, box.title(), x + 4, y + 3, 0xFFFFFFFF);
        int ly = y + 15;
        for (Component line : box.lines()) {
            context.text(tr, line, x + 4, ly, 0xFFFFFFFF);
            ly += 10;
        }
    }
}
