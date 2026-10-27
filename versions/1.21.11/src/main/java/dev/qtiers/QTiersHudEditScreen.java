package dev.qtiers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Drag the opponent HUD to where you want it. Shows your own tiers as a preview. */
public class QTiersHudEditScreen extends Screen {
    private final @Nullable Screen parent;
    private boolean dragging;
    /** Where inside the box it was grabbed, so it doesn't jump to the cursor. */
    private double grabX, grabY;

    public QTiersHudEditScreen(@Nullable Screen parent) {
        super(Text.literal("Move opponent HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> {
            QTiersConfig.get().opponentHudX = 0;
            QTiersConfig.get().opponentHudY = 0;
        }).dimensions(width / 2 - 102, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 + 2, y, 100, 20).build());
    }

    private OpponentHud.Box previewBox() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) return OpponentHud.build(client.player.getUuid(), client.player.getGameProfile().name());
        return OpponentHud.build(new UUID(0, 0), "Opponent");
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Keep the game visible in a world so the HUD can be placed against it
        if (client == null || client.world == null) super.renderBackground(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        OpponentHud.Box box = previewBox();
        int[] pos = OpponentHud.position(box, width, height);
        OpponentHud.draw(context, box, pos[0], pos[1]);
        boolean hover = dragging || inside(box, pos, mouseX, mouseY);
        context.drawStrokedRectangle(pos[0] - 1, pos[1] - 1, box.width() + 2, box.height() + 2, hover ? 0xFFFFFF55 : 0x80FFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Drag the box to move the opponent HUD").formatted(Formatting.GRAY),
                width / 2, height - 42, 0xFFFFFFFF);
    }

    private static boolean inside(OpponentHud.Box box, int[] pos, double mx, double my) {
        return mx >= pos[0] && mx < pos[0] + box.width() && my >= pos[1] && my < pos[1] + box.height();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        OpponentHud.Box box = previewBox();
        int[] pos = OpponentHud.position(box, width, height);
        if (click.button() == 0 && inside(box, pos, click.x(), click.y())) {
            dragging = true;
            grabX = click.x() - pos[0];
            grabY = click.y() - pos[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
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
    public boolean mouseReleased(Click click) {
        if (dragging) {
            dragging = false;
            QTiersConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        QTiersConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
