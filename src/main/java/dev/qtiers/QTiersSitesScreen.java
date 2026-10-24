package dev.qtiers;

import dev.qtiers.QTiersConfig.Position;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;

import static dev.qtiers.QTiersConfigScreen.BUTTON_H;
import static dev.qtiers.QTiersConfigScreen.BUTTON_W;
import static dev.qtiers.QTiersConfigScreen.GAP;

/** "Tier lists..." page of the settings: where each site's tier goes and which gamemode it shows. */
public class QTiersSitesScreen extends Screen {
    private final Screen parent;

    public QTiersSitesScreen(Screen parent) {
        super(Text.literal("Tier lists"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QTiersConfig config = QTiersConfig.get();
        int left = width / 2 - BUTTON_W - GAP / 2;
        int right = width / 2 + GAP / 2;
        int rows = TierSource.values().length;
        int y = Math.max(40, (height - rows * (BUTTON_H + GAP) - 8 - BUTTON_H) / 2 + 6);

        // One row per site: position | gamemode
        for (TierSource source : TierSource.values()) {
            QTiersConfig.SiteSettings site = config.site(source);
            Text label = Text.literal(source.displayName).styled(s -> s.withColor(source.color));
            addDrawableChild(CyclingButtonWidget.<Position>builder(QTiersSitesScreen::positionText, site.position)
                    .values(Arrays.asList(Position.values()))
                    .build(left, y, BUTTON_W, BUTTON_H, label, (b, v) -> {
                        site.position = v;
                        QTiersConfig.save();
                    }));
            List<String> modes = GameModes.cycleFor(source);
            addDrawableChild(CyclingButtonWidget.<String>builder(mode -> GameModes.styledName(source, mode), site.gamemode)
                    .values(modes)
                    .build(right, y, BUTTON_W, BUTTON_H, Text.literal("Mode"), (b, v) -> {
                        site.gamemode = v;
                        QTiersConfig.save();
                    }));
            y += BUTTON_H + GAP;
        }

        y += 8;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 - 100, Math.min(y, height - 28), 200, BUTTON_H).build());
    }

    private static Text positionText(Position p) {
        return switch (p) {
            case LEFT -> Text.literal("Left");
            case RIGHT -> Text.literal("Right");
            case OFF -> Text.literal("Off").formatted(Formatting.RED);
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Site position  ·  gamemode shown").formatted(Formatting.GRAY), width / 2, 27, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        QTiersConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
