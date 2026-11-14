package dev.qtiers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
        return height - 40;
    }

    protected abstract int rowCount();

    protected abstract void renderRow(DrawContext context, int index, int x, int y, int width, boolean hovered);

    /** Text drawn above the rows (column names); null for none. */
    protected void renderHeader(DrawContext context, int x, int y, int width) {}

    /** Shown in place of the rows when there are none (e.g. "Loading..."). */
    protected Text emptyText() {
        return Text.literal("Nothing to show");
    }

    /** Theme colors for rows: normal, secondary and faint text, and hovered names. */
    protected static int textColor() {
        return Theme.palette().text();
    }

    protected static int dimColor() {
        return Theme.palette().textDim();
    }

    protected static int mutedColor() {
        return Theme.palette().textMuted();
    }

    protected static int hoverColor() {
        return Theme.accent();
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
        addDrawableChild(Ui.primary(width / 2 - 100, height - 30, 200, 20, Text.translatable("gui.done"), b -> close()));
    }

    private int maxScroll() {
        return Math.max(0, rowCount() * ROW_H - (listBottom() - listTop()));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        Theme.Palette p = Theme.palette();
        int x = listX(), w = listWidth(), top = listTop(), bottom = listBottom();
        Ui.window(context, x - 10, 4, w + 20, height - 8, title);
        super.render(context, mouseX, mouseY, deltaTicks);
        renderHeader(context, x, top - 12, w);
        Ui.rect(context, x - 3, top - 3, w + 6, bottom - top + 6, Theme.radius(), p.card());
        if (rowCount() == 0) {
            Ui.centered(context, emptyText(), width / 2, top + 10, p.textDim());
            return;
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        context.enableScissor(x - 2, top, x + w + 2, bottom);
        int first = (int) (scroll / ROW_H);
        for (int i = first; i < rowCount(); i++) {
            int y = top + i * ROW_H - (int) scroll;
            if (y > bottom) break;
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= Math.max(y, top) && mouseY < Math.min(y + ROW_H, bottom);
            if (hovered) context.fill(x - 2, y, x + w + 2, y + ROW_H, Theme.alpha(Theme.accent(), 0x30));
            renderRow(context, i, x, y + 2, w, hovered);
        }
        context.disableScissor();
        if (maxScroll() > 0) {
            // Scrollbar
            int track = bottom - top;
            int thumb = Math.max(10, track * track / (rowCount() * ROW_H));
            int thumbY = top + (int) ((track - thumb) * scroll / maxScroll());
            Ui.rect(context, x + w + 4, thumbY, 3, thumb, 1, p.textMuted());
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
        Ui.clipped(context, text, x, y, maxWidth, color);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
