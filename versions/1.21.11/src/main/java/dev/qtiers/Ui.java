package dev.qtiers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Drawing helpers for the themed QTiers screens: rounded panels, switches, sliders and buttons. */
public final class Ui {
    private Ui() {}

    private static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private static int inset(int r, int row) {
        double dy = r - row - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    /** Filled rectangle with rounded corners. */
    public static void rect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            c.fill(x, y, x + w, y + h, color);
            return;
        }
        c.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int in = inset(r, i);
            c.fill(x + in, y + i, x + w - in, y + i + 1, color);
            c.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, color);
        }
    }

    /** One-pixel rounded outline. */
    public static void outline(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        c.fill(x + r, y, x + w - r, y + 1, color);
        c.fill(x + r, y + h - 1, x + w - r, y + h, color);
        c.fill(x, y + r, x + 1, y + h - r, color);
        c.fill(x + w - 1, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int from = inset(r, i);
            int to = Math.max(from + 1, i == 0 ? r : inset(r, i - 1));
            c.fill(x + from, y + i, x + to, y + i + 1, color);
            c.fill(x + w - to, y + i, x + w - from, y + i + 1, color);
            c.fill(x + from, y + h - 1 - i, x + to, y + h - i, color);
            c.fill(x + w - to, y + h - 1 - i, x + w - from, y + h - i, color);
        }
    }

    /** Theme text: no shadow, like a modern UI; colors from styled parts of the text win. */
    public static void text(DrawContext c, Text text, int x, int y, int color) {
        c.drawText(font(), text, x, y, color, false);
    }

    public static void text(DrawContext c, String text, int x, int y, int color) {
        c.drawText(font(), text, x, y, color, false);
    }

    public static void centered(DrawContext c, Text text, int cx, int y, int color) {
        c.drawText(font(), text, cx - font().getWidth(text) / 2, y, color, false);
    }

    /** Text cut with "…" to fit {@code maxWidth}. */
    public static void clipped(DrawContext c, Text text, int x, int y, int maxWidth, int color) {
        TextRenderer f = font();
        if (f.getWidth(text) <= maxWidth) {
            text(c, text, x, y, color);
        } else {
            String s = f.trimToWidth(text.getString(), Math.max(0, maxWidth - f.getWidth("…"))) + "…";
            text(c, Text.literal(s).setStyle(text.getStyle()), x, y, color);
        }
    }

    /** On/off switch, 22x12. {@code t} runs from 0 (off) to 1 (on) so it can animate. */
    public static void toggle(DrawContext c, int x, int y, float t, boolean hovered) {
        Theme.Palette p = Theme.palette();
        int track = Theme.mix(hovered ? Theme.mix(p.control(), p.text(), 0.12f) : p.control(), Theme.accent(), t);
        rect(c, x, y, 22, 12, 6, track);
        int knobX = x + 2 + Math.round(10 * t);
        rect(c, knobX, y + 2, 8, 8, 4, 0xFFFFFFFF);
    }

    /** Slider track with the filled part in the accent color and a round knob. */
    public static void slider(DrawContext c, int x, int y, int w, double fraction, boolean active) {
        Theme.Palette p = Theme.palette();
        int filled = (int) Math.round(w * Math.max(0, Math.min(1, fraction)));
        rect(c, x, y + 4, w, 4, 2, p.control());
        rect(c, x, y + 4, Math.max(4, filled), 4, 2, Theme.accent());
        int knob = Math.max(x, Math.min(x + w - 10, x + filled - 5));
        rect(c, knob, y + 1, 10, 10, 5, active ? Theme.mix(0xFFFFFFFF, Theme.accent(), 0.25f) : 0xFFFFFFFF);
    }

    /** Button body: accent-filled when primary, card colored otherwise. */
    public static void button(DrawContext c, int x, int y, int w, int h, Text label, boolean hovered, boolean active, boolean primary) {
        Theme.Palette p = Theme.palette();
        int bg;
        int fg;
        if (primary) {
            bg = hovered ? Theme.mix(Theme.accent(), 0xFFFFFFFF, 0.15f) : Theme.accent();
            fg = Theme.onAccent();
        } else {
            bg = hovered ? Theme.mix(p.control(), p.text(), 0.12f) : p.control();
            fg = p.text();
        }
        if (!active) {
            bg = Theme.alpha(bg, 0x80);
            fg = p.textMuted();
        }
        rect(c, x, y, w, h, Theme.radius(), bg);
        TextRenderer f = font();
        int tw = f.getWidth(label);
        if (tw > w - 6) {
            clipped(c, label, x + 3, y + (h - 8) / 2, w - 6, fg);
        } else {
            text(c, label, x + (w - tw) / 2, y + (h - 8) / 2, fg);
        }
    }

    /** A vanilla button drawn in the theme, so screens keep keyboard navigation and narration. */
    public static final class ThemedButton extends ButtonWidget {
        private final boolean primary;

        public ThemedButton(int x, int y, int w, int h, net.minecraft.text.Text message, PressAction onPress, boolean primary) {
            super(x, y, w, h, message, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.primary = primary;
        }

        @Override
        protected void drawIcon(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
            button(context, getX(), getY(), getWidth(), getHeight(), getMessage(), isSelected(), active, primary);
        }
    }

    public static ThemedButton button(int x, int y, int w, int h, String label, ButtonWidget.PressAction onPress) {
        return new ThemedButton(x, y, w, h, net.minecraft.text.Text.literal(label), onPress, false);
    }

    public static ThemedButton primary(int x, int y, int w, int h, Text label, ButtonWidget.PressAction onPress) {
        return new ThemedButton(x, y, w, h, label, onPress, true);
    }

    /** Search box without vanilla's frame; draw {@link #searchBox} behind it. */
    public static TextFieldWidget searchField(TextRenderer tr, int x, int y, int w, TextFieldWidget previous) {
        TextFieldWidget field = new TextFieldWidget(tr, x + 16, y + 6, w - 20, 10, Text.literal("Search player"));
        field.setDrawsBackground(false);
        field.setMaxLength(16);
        field.setTextPredicate(t -> t.matches("[A-Za-z0-9_]*"));
        field.setEditableColor(Theme.palette().text());
        if (previous != null) field.setText(previous.getText());
        return field;
    }

    /** Rounded frame and magnifier glyph for a {@link #searchField} placed at the same x, y, w (height 20). */
    public static void searchBox(DrawContext c, TextFieldWidget field, int x, int y, int w) {
        Theme.Palette p = Theme.palette();
        boolean focused = field.isFocused();
        rect(c, x, y, w, 20, Theme.radius(), p.card());
        outline(c, x, y, w, 20, Theme.radius(), focused ? Theme.accent() : p.border());
        text(c, Text.literal("⌕").formatted(Formatting.BOLD), x + 5, y + 6, p.textMuted());
        // Own placeholder: vanilla's is drawn with a shadow, which looks wrong on the light style
        if (field.getText().isEmpty() && !focused) text(c, "Search player...", x + 16, y + 6, p.textMuted());
    }

    /** Panel behind a whole screen: rounded, bordered, with a header strip. */
    public static void window(DrawContext c, int x, int y, int w, int h, Text title) {
        Theme.Palette p = Theme.palette();
        rect(c, x, y, w, h, Theme.radius() + 2, p.panel());
        outline(c, x, y, w, h, Theme.radius() + 2, p.border());
        centered(c, title.copy().formatted(Formatting.BOLD), x + w / 2, y + 8, p.text());
        c.fill(x + w / 2 - 12, y + 19, x + w / 2 + 12, y + 20, Theme.accent());
    }
}
