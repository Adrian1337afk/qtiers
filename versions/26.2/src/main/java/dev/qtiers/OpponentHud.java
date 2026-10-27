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

/** Small top-left box with the tiers of the last player you hit, shown for a few seconds after each hit. */
public final class OpponentHud {
    private static final long SHOW_MS = 10_000;

    private static UUID target;
    private static String targetName;
    private static long lastHit;

    private OpponentHud() {}

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

        List<Component> lines = new ArrayList<>();
        for (TierSource source : TierSource.values()) {
            Optional<Ranking> ranking = TierApi.getCached(target, source)
                    .flatMap(p -> TierDisplay.pick(p, config.site(source).gamemode, config));
            if (ranking.isEmpty()) continue;
            Ranking r = ranking.get();
            MutableComponent line = Component.literal(source.shortName + " ").withStyle(s -> s.withColor(source.color));
            if (config.showIcons) line.append(ModeTexts.icon(source, r.mode()));
            line.append(Component.literal(r.label()).withStyle(s -> s.withColor(r.color())))
                    .append(Component.literal(" " + GameModes.get(r.mode()).title()).withStyle(s -> s.withColor(0xBBBBBB)));
            lines.add(line);
        }
        Component title = Component.literal(targetName).withStyle(s -> s.withBold(true));
        if (lines.isEmpty()) lines.add(Component.literal("No tiers").withStyle(s -> s.withColor(0x888888)));

        var tr = client.font;
        int w = tr.width(title);
        for (Component line : lines) w = Math.max(w, tr.width(line));
        int x = 4, y = 4, h = 14 + lines.size() * 10;
        context.fill(x, y, x + w + 8, y + h, 0x90000000);
        context.text(tr, title, x + 4, y + 3, 0xFFFFFFFF);
        int ly = y + 15;
        for (Component line : lines) {
            context.text(tr, line, x + 4, ly, 0xFFFFFFFF);
            ly += 10;
        }
    }
}
