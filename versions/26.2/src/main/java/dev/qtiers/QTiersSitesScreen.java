package dev.qtiers;

import dev.qtiers.QTiersConfig.HighestMode;
import dev.qtiers.QTiersConfig.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.Arrays;
import java.util.List;

import static dev.qtiers.QTiersConfigScreen.BUTTON_H;
import static dev.qtiers.QTiersConfigScreen.BUTTON_W;
import static dev.qtiers.QTiersConfigScreen.GAP;

/** "Tiers" page of the settings: where each site's tier goes and which gamemode it shows. */
public class QTiersSitesScreen extends Screen {
    private final Screen parent;

    public QTiersSitesScreen(Screen parent) {
        super(Component.literal("Tier lists"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QTiersConfig config = QTiersConfig.get();
        int left = width / 2 - BUTTON_W - GAP / 2;
        int right = width / 2 + GAP / 2;
        int rows = TierSource.values().length + 1;
        int y = Math.max(40, (height - rows * (BUTTON_H + GAP) - 8 - BUTTON_H) / 2 + 6);

        // One row per site: position | gamemode
        for (TierSource source : TierSource.values()) {
            QTiersConfig.SiteSettings site = config.site(source);
            Component label = Component.literal(source.displayName).withStyle(s -> s.withColor(source.color));
            addRenderableWidget(CycleButton.<Position>builder(QTiersSitesScreen::positionText, site.position)
                    .withValues(Arrays.asList(Position.values()))
                    .create(left, y, BUTTON_W, BUTTON_H, label, (b, v) -> {
                        site.position = v;
                        QTiersConfig.save();
                    }));
            List<String> modes = GameModes.cycleFor(source);
            addRenderableWidget(CycleButton.<String>builder(mode -> ModeTexts.styledName(source, mode), site.gamemode)
                    .withValues(modes)
                    .create(right, y, BUTTON_W, BUTTON_H, Component.literal("Mode"), (b, v) -> {
                        site.gamemode = v;
                        QTiersConfig.save();
                    }));
            y += BUTTON_H + GAP;
        }

        addRenderableWidget(CycleButton.<HighestMode>builder(QTiersSitesScreen::highestText, config.highestMode)
                .withValues(Arrays.asList(HighestMode.values()))
                .create(left, y, BUTTON_W * 2 + GAP, BUTTON_H, Component.literal("Show highest tier"), (b, v) -> {
                    config.highestMode = v;
                    QTiersConfig.save();
                }));
        y += BUTTON_H + GAP;

        y += 8;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 100, Math.min(y, height - 28), 200, BUTTON_H).build());
    }

    private static Component highestText(HighestMode m) {
        return switch (m) {
            case NEVER -> Component.literal("Never");
            case NOT_FOUND -> Component.literal("If not ranked in mode");
            case ALWAYS -> Component.literal("Always");
        };
    }

    private static Component positionText(Position p) {
        return switch (p) {
            case LEFT -> Component.literal("Left");
            case RIGHT -> Component.literal("Right");
            case OFF -> Component.literal("Off").withStyle(ChatFormatting.RED);
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        context.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);
        context.centeredText(font,
                Component.literal("Site position  ·  gamemode shown").withStyle(ChatFormatting.GRAY), width / 2, 27, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        QTiersConfig.save();
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
