package dev.qtiers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** In-game settings, opened with /tiers, /qtiers or the config keybind. Per-site options live in {@link QTiersSitesScreen}. */
public class QTiersConfigScreen extends Screen {
    static final int BUTTON_W = 150;
    static final int BUTTON_H = 20;
    static final int GAP = 4;

    private final @Nullable Screen parent;
    private TextFieldWidget search;

    public QTiersConfigScreen(@Nullable Screen parent) {
        super(Text.literal("QTiers Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        QTiersConfig config = QTiersConfig.get();
        int left = width / 2 - BUTTON_W - GAP / 2;
        int right = width / 2 + GAP / 2;
        // Content is 8 rows plus a gap; centre it, but never above the title
        int y = Math.max(30, (height - 8 * (BUTTON_H + GAP) - 8) / 2 + 6);

        // Top row: Tiers (per-site options) | player search box
        addDrawableChild(ButtonWidget.builder(Text.literal("Tiers"),
                        b -> client.setScreen(new QTiersSitesScreen(this)))
                .dimensions(left, y, BUTTON_W, BUTTON_H).build());
        search = addDrawableChild(QTiersProfileScreen.searchField(textRenderer, right, y, BUTTON_W, search));
        y += BUTTON_H + GAP;
        addDrawableChild(ButtonWidget.builder(Text.literal("Leaderboards"),
                        b -> client.setScreen(new QTiersLeaderboardScreen(this)))
                .dimensions(left, y, BUTTON_W, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Online players"),
                        b -> client.setScreen(new QTiersLobbyScreen(this)))
                .dimensions(right, y, BUTTON_W, BUTTON_H).build());
        y += BUTTON_H + GAP;

        y = toggle(left, y, "Enabled", config.enabled, v -> config.enabled = v, false);
        y = toggle(right, y, "Gamemode icons", config.showIcons, v -> config.showIcons = v, true);
        y = toggle(left, y, "Nametags", config.showInNametags, v -> config.showInNametags = v, false);
        y = toggle(right, y, "Tab list", config.showInTabList, v -> config.showInTabList = v, true);
        y = toggle(left, y, "Site labels", config.showSiteLabel, v -> config.showSiteLabel = v, false);
        y = toggle(right, y, "Colored separator", config.coloredSeparator, v -> config.coloredSeparator = v, true);
        y = toggle(left, y, "Show retired", config.showRetired, v -> config.showRetired = v, false);
        y = toggle(right, y, "Own name in F5", config.showOwnNametag, v -> config.showOwnNametag = v, true);
        // Opponent HUD toggle shares its slot with a small "Move" button
        addDrawableChild(CyclingButtonWidget.onOffBuilder(config.opponentHud)
                .build(left, y, BUTTON_W - 44, BUTTON_H, Text.literal("Opponent HUD"), (b, v) -> {
                    config.opponentHud = v;
                    QTiersConfig.save();
                }));
        addDrawableChild(ButtonWidget.builder(Text.literal("Move"), b -> client.setScreen(new QTiersHudEditScreen(this)))
                .dimensions(left + BUTTON_W - 42, y, 42, BUTTON_H).build());
        y = toggle(right, y, "Clickable chat names", config.clickableNames, v -> config.clickableNames = v, true);
        y += 8;

        int bottom = Math.min(y, height - 28);
        addDrawableChild(ButtonWidget.builder(Text.literal("Refresh tiers"), b -> TierApi.clearCache())
                .dimensions(left, bottom, BUTTON_W, BUTTON_H).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(right, bottom, BUTTON_W, BUTTON_H).build());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (QTiersProfileScreen.searchEntered(search, keyCode)) {
            QTiersConfig.save();
            client.setScreen(new QTiersProfileScreen(search.getText()));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Adds an on/off button; returns the next row's y once the right column is filled. */
    private int toggle(int x, int y, String name, boolean value, Consumer<Boolean> setter, boolean endsRow) {
        addDrawableChild(CyclingButtonWidget.onOffBuilder(value)
                .build(x, y, BUTTON_W, BUTTON_H, Text.literal(name), (b, v) -> {
                    setter.accept(v);
                    QTiersConfig.save();
                }));
        return endsRow ? y + BUTTON_H + GAP : y;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        QTiersConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
