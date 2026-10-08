package dev.qtiers;

import dev.qtiers.QTiersConfig.HighestMode;
import dev.qtiers.QTiersConfig.Position;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** In-game settings, opened with /tiers, /qtiers or the config keybind. */
public class QTiersConfigScreen extends Screen {
    private static final int BUTTON_W = 150;
    private static final int BUTTON_H = 20;
    private static final int GAP = 4;
    // Full layout needs ~292px; shorter windows (e.g. GUI scale 2 on 480p) use tight spacing and no subtitle
    private static final int FULL_LAYOUT_HEIGHT = 296;

    private int rowGap = GAP;
    private boolean compact;

    private final @Nullable Screen parent;

    public QTiersConfigScreen(@Nullable Screen parent) {
        super(Text.literal("QTiers Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QTiersConfig config = QTiersConfig.get();
        int left = width / 2 - BUTTON_W - GAP / 2;
        int right = width / 2 + GAP / 2;
        compact = height < FULL_LAYOUT_HEIGHT;
        rowGap = compact ? 1 : GAP;
        int sectionGap = compact ? 1 : 8;
        int y = compact ? 17 : 40;

        // One row per site: position | gamemode
        for (TierSource source : TierSource.values()) {
            QTiersConfig.SiteSettings site = config.site(source);
            Text label = Text.literal(source.displayName).styled(s -> s.withColor(source.color));
            addDrawableChild(CyclingButtonWidget.<Position>builder(QTiersConfigScreen::positionText, site.position)
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
            y += BUTTON_H + rowGap;
        }

        y += sectionGap;
        addDrawableChild(CyclingButtonWidget.<HighestMode>builder(QTiersConfigScreen::highestText, config.highestMode)
                .values(Arrays.asList(HighestMode.values()))
                .build(left, y, BUTTON_W * 2 + GAP, BUTTON_H, Text.literal("Show highest tier"), (b, v) -> {
                    config.highestMode = v;
                    QTiersConfig.save();
                }));
        y += BUTTON_H + rowGap;

        y = toggle(left, y, "Enabled", config.enabled, v -> config.enabled = v, false);
        y = toggle(right, y, "Gamemode icons", config.showIcons, v -> config.showIcons = v, true);
        y = toggle(left, y, "Nametags", config.showInNametags, v -> config.showInNametags = v, false);
        y = toggle(right, y, "Tab list", config.showInTabList, v -> config.showInTabList = v, true);
        y = toggle(left, y, "Site labels", config.showSiteLabel, v -> config.showSiteLabel = v, false);
        y = toggle(right, y, "Colored separator", config.coloredSeparator, v -> config.coloredSeparator = v, true);
        y = toggle(left, y, "Show retired", config.showRetired, v -> config.showRetired = v, false);
        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh tiers"), b -> TierApi.clearCache())
                .dimensions(right, y, BUTTON_W, BUTTON_H).build());
        y += BUTTON_H + rowGap + sectionGap;

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 - 100, Math.min(y, height - 28), 200, BUTTON_H).build());
    }

    /** Adds an on/off button; returns the next row's y once the right column is filled. */
    private int toggle(int x, int y, String name, boolean value, Consumer<Boolean> setter, boolean endsRow) {
        addDrawableChild(CyclingButtonWidget.onOffBuilder(value)
                .build(x, y, BUTTON_W, BUTTON_H, Text.literal(name), (b, v) -> {
                    setter.accept(v);
                    QTiersConfig.save();
                }));
        return endsRow ? y + BUTTON_H + rowGap : y;
    }

    private static Text positionText(Position p) {
        return switch (p) {
            case LEFT -> Text.literal("Left");
            case RIGHT -> Text.literal("Right");
            case OFF -> Text.literal("Off").formatted(Formatting.RED);
        };
    }

    private static Text highestText(HighestMode m) {
        return switch (m) {
            case NEVER -> Text.literal("Never");
            case NOT_FOUND -> Text.literal("If not ranked in mode");
            case ALWAYS -> Text.literal("Always");
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, compact ? 5 : 15, 0xFFFFFFFF);
        if (!compact) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Site position  ·  gamemode shown").formatted(Formatting.GRAY), width / 2, 27, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        QTiersConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
