package dev.qtiers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** In-game settings, opened with /tiers, /qtiers or the config keybind. Per-site options live in {@link QTiersSitesScreen}. */
public class QTiersConfigScreen extends Screen {
    static final int BUTTON_W = 150;
    static final int BUTTON_H = 20;
    static final int GAP = 4;

    private final @Nullable Screen parent;
    private EditBox search;

    public QTiersConfigScreen(@Nullable Screen parent) {
        super(Component.literal("QTiers Settings"));
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
        addRenderableWidget(Button.builder(Component.literal("Tiers"),
                        b -> minecraft.gui.setScreen(new QTiersSitesScreen(this)))
                .bounds(left, y, BUTTON_W, BUTTON_H).build());
        search = addRenderableWidget(QTiersProfileScreen.searchField(font, right, y, BUTTON_W, search));
        y += BUTTON_H + GAP;
        addRenderableWidget(Button.builder(Component.literal("Leaderboards"),
                        b -> minecraft.gui.setScreen(new QTiersLeaderboardScreen(this)))
                .bounds(left, y, BUTTON_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.literal("Online players"),
                        b -> minecraft.gui.setScreen(new QTiersLobbyScreen(this)))
                .bounds(right, y, BUTTON_W, BUTTON_H).build());
        y += BUTTON_H + GAP;

        y = toggle(left, y, "Enabled", config.enabled, v -> config.enabled = v, false);
        y = toggle(right, y, "Gamemode icons", config.showIcons, v -> config.showIcons = v, true);
        y = toggle(left, y, "Nametags", config.showInNametags, v -> config.showInNametags = v, false);
        y = toggle(right, y, "Tab list", config.showInTabList, v -> config.showInTabList = v, true);
        y = toggle(left, y, "Site labels", config.showSiteLabel, v -> config.showSiteLabel = v, false);
        y = toggle(right, y, "Colored separator", config.coloredSeparator, v -> config.coloredSeparator = v, true);
        y = toggle(left, y, "Show retired", config.showRetired, v -> config.showRetired = v, false);
        y = toggle(right, y, "Own name in F5", config.showOwnNametag, v -> config.showOwnNametag = v, true);
        y = toggle(left, y, "Opponent HUD", config.opponentHud, v -> config.opponentHud = v, false);
        y = toggle(right, y, "Clickable chat names", config.clickableNames, v -> config.clickableNames = v, true);
        y += 8;

        int bottom = Math.min(y, height - 28);
        addRenderableWidget(Button.builder(Component.literal("Refresh tiers"), b -> TierApi.clearCache())
                .bounds(left, bottom, BUTTON_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(right, bottom, BUTTON_W, BUTTON_H).build());
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (QTiersProfileScreen.searchEntered(search, input)) {
            QTiersConfig.save();
            minecraft.gui.setScreen(new QTiersProfileScreen(search.getValue()));
            return true;
        }
        return super.keyPressed(input);
    }

    /** Adds an on/off button; returns the next row's y once the right column is filled. */
    private int toggle(int x, int y, String name, boolean value, Consumer<Boolean> setter, boolean endsRow) {
        addRenderableWidget(CycleButton.onOffBuilder(value)
                .create(x, y, BUTTON_W, BUTTON_H, Component.literal(name), (b, v) -> {
                    setter.accept(v);
                    QTiersConfig.save();
                }));
        return endsRow ? y + BUTTON_H + GAP : y;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        context.centeredText(font, title, width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        QTiersConfig.save();
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
