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

/** Small top-left box with the tiers of the last player you hit, shown for a few seconds after each hit. */
public final class OpponentHud {
    private static final long SHOW_MS = 10_000;

    private static UUID target;
    private static String targetName;
    private static long lastHit;

    private OpponentHud() {}

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

        List<Text> lines = new ArrayList<>();
        for (TierSource source : TierSource.values()) {
            Optional<Ranking> ranking = TierApi.getCached(target, source)
                    .flatMap(p -> TierDisplay.pick(p, config.site(source).gamemode, config));
            if (ranking.isEmpty()) continue;
            Ranking r = ranking.get();
            MutableText line = Text.literal(source.shortName + " ").styled(s -> s.withColor(source.color));
            if (config.showIcons) line.append(GameModes.icon(source, r.mode()));
            line.append(Text.literal(r.label()).styled(s -> s.withColor(r.color())))
                    .append(Text.literal(" " + GameModes.get(r.mode()).title()).styled(s -> s.withColor(0xBBBBBB)));
            lines.add(line);
        }
        Text title = Text.literal(targetName).styled(s -> s.withBold(true));
        if (lines.isEmpty()) lines.add(Text.literal("No tiers").styled(s -> s.withColor(0x888888)));

        var tr = client.textRenderer;
        int w = tr.getWidth(title);
        for (Text line : lines) w = Math.max(w, tr.getWidth(line));
        int x = 4, y = 4, h = 14 + lines.size() * 10;
        context.fill(x, y, x + w + 8, y + h, 0x90000000);
        context.drawTextWithShadow(tr, title, x + 4, y + 3, 0xFFFFFFFF);
        int ly = y + 15;
        for (Text line : lines) {
            context.drawTextWithShadow(tr, line, x + 4, ly, 0xFFFFFFFF);
            ly += 10;
        }
    }
}
