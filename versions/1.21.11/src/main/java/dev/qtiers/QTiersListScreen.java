package dev.qtiers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Base for the scrolling list screens (online players, leaderboards, compare): a title, an
 * optional header line, rows that scroll with the mouse wheel, and a Done button.
 */
abstract class QTiersListScreen extends Screen {
    protected static final int ROW_H = 12;
    private final @Nullable Screen parent;
    private double scroll;

    protected QTiersListScreen(Text title, @Nullable Screen parent) {
        super(title);
        this.parent = parent;
    }

    /** Where the rows start; subclasses with controls above the list move it down. */
    protected int listTop() {
        return 44;
    }

    protected int listBottom() {
        return height - 34;
    }

    protected abstract int rowCount();

    protected abstract void renderRow(DrawContext context, int index, int x, int y, int width, boolean hovered);

    /** Text drawn above the rows (column names); null for none. */
    protected void renderHeader(DrawContext context, int x, int y, int width) {}

    /** Shown in place of the rows when there are none (e.g. "Loading..."). */
    protected Text emptyText() {
        return Text.literal("Nothing to show");
    }

    protected boolean rowClicked(int index) {
        return false;
    }

    protected int listX() {
        return Math.max(10, width / 2 - 210);
    }

    protected int listWidth() {
        return Math.min(width - 20, 420);
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> close())
                .dimensions(width / 2 - 100, height - 27, 200, 20).build());
    }

    private int maxScroll() {
        return Math.max(0, rowCount() * ROW_H - (listBottom() - listTop()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        int x = listX(), w = listWidth(), top = listTop(), bottom = listBottom();
        renderHeader(context, x, top - 12, w);
        context.fill(x - 2, top - 2, x + w + 2, bottom + 2, 0x60000000);
        if (rowCount() == 0) {
            context.drawCenteredTextWithShadow(textRenderer, emptyText(), width / 2, top + 10, 0xFFAAAAAA);
            return;
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        context.enableScissor(x - 2, top, x + w + 2, bottom);
        int first = (int) (scroll / ROW_H);
        for (int i = first; i < rowCount(); i++) {
            int y = top + i * ROW_H - (int) scroll;
            if (y > bottom) break;
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= Math.max(y, top) && mouseY < Math.min(y + ROW_H, bottom);
            if (hovered) context.fill(x - 2, y, x + w + 2, y + ROW_H, 0x30FFFFFF);
            renderRow(context, i, x, y + 2, w, hovered);
        }
        context.disableScissor();
        if (maxScroll() > 0) {
            // Scrollbar
            int track = bottom - top;
            int thumb = Math.max(10, track * track / (rowCount() * ROW_H));
            int thumbY = top + (int) ((track - thumb) * scroll / maxScroll());
            context.fill(x + w + 3, thumbY, x + w + 5, thumbY + thumb, 0xA0FFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(scroll - verticalAmount * ROW_H * 3, maxScroll()));
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        int x = listX(), w = listWidth(), top = listTop(), bottom = listBottom();
        if (click.button() == 0 && click.x() >= x && click.x() < x + w && click.y() >= top && click.y() < bottom) {
            int index = (int) ((click.y() - top + scroll) / ROW_H);
            if (index >= 0 && index < rowCount()) return rowClicked(index);
        }
        return false;
    }

    /** Opens a player's profile on top of this list. */
    protected void openProfile(String name) {
        MinecraftClient.getInstance().setScreen(new QTiersProfileScreen(name, this));
    }

    /** Draws text cut to fit {@code maxWidth}, so long names don't run into the next column. */
    protected void drawClipped(DrawContext context, Text text, int x, int y, int maxWidth, int color) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
        } else {
            String s = textRenderer.trimToWidth(text.getString(), maxWidth - textRenderer.getWidth("…")) + "…";
            context.drawTextWithShadow(textRenderer, Text.literal(s).setStyle(text.getStyle()), x, y, color);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
