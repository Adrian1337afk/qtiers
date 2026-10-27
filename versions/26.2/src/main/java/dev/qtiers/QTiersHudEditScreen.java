package dev.qtiers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Drag the opponent HUD to where you want it. Shows your own tiers as a preview. */
public class QTiersHudEditScreen extends Screen {
    private final @Nullable Screen parent;
    private boolean dragging;
    /** Where inside the box it was grabbed, so it doesn't jump to the cursor. */
    private double grabX, grabY;

    public QTiersHudEditScreen(@Nullable Screen parent) {
        super(Component.literal("Move opponent HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = height - 28;
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            QTiersConfig.get().opponentHudX = 0;
            QTiersConfig.get().opponentHudY = 0;
        }).bounds(width / 2 - 102, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 + 2, y, 100, 20).build());
    }

    private OpponentHud.Box previewBox() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) return OpponentHud.build(client.player.getUUID(), client.player.getGameProfile().name());
        return OpponentHud.build(new UUID(0, 0), "Opponent");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        // Keep the game visible in a world so the HUD can be placed against it
        if (minecraft.level == null) super.extractBackground(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks) {
        super.extractRenderState(context, mouseX, mouseY, deltaTicks);
        OpponentHud.Box box = previewBox();
        int[] pos = OpponentHud.position(box, width, height);
        OpponentHud.draw(context, box, pos[0], pos[1]);
        boolean hover = dragging || inside(box, pos, mouseX, mouseY);
        context.outline(pos[0] - 1, pos[1] - 1, box.width() + 2, box.height() + 2, hover ? 0xFFFFFF55 : 0x80FFFFFF);
        context.centeredText(font, Component.literal("Drag the box to move the opponent HUD").withStyle(ChatFormatting.GRAY),
                width / 2, height - 42, 0xFFFFFFFF);
    }

    private static boolean inside(OpponentHud.Box box, int[] pos, double mx, double my) {
        return mx >= pos[0] && mx < pos[0] + box.width() && my >= pos[1] && my < pos[1] + box.height();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        OpponentHud.Box box = previewBox();
        int[] pos = OpponentHud.position(box, width, height);
        if (click.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && inside(box, pos, click.x(), click.y())) {
            dragging = true;
            grabX = click.x() - pos[0];
            grabY = click.y() - pos[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (!dragging) return super.mouseDragged(click, offsetX, offsetY);
        OpponentHud.Box box = previewBox();
        int freeX = Math.max(1, width - box.width() - 2 * OpponentHud.MARGIN);
        int freeY = Math.max(1, height - box.height() - 2 * OpponentHud.MARGIN);
        QTiersConfig config = QTiersConfig.get();
        config.opponentHudX = OpponentHud.clamp((click.x() - grabX - OpponentHud.MARGIN) / freeX);
        config.opponentHudY = OpponentHud.clamp((click.y() - grabY - OpponentHud.MARGIN) / freeY);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging) {
            dragging = false;
            QTiersConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void onClose() {
        QTiersConfig.save();
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
