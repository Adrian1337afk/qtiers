package dev.qtiers;

import dev.qtiers.QTiersConfig.HighestMode;
import dev.qtiers.QTiersConfig.Position;
import dev.qtiers.QTiersConfig.UiStyle;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Settings window: a sidebar with pages on the left, option cards on the right.
 * Opened with /qtiers, the config keybind or Mod Menu.
 */
public class QTiersConfigScreen extends Screen {
    private static final Identifier LOGO = Identifier.of(QTiers.MOD_ID, "icon.png");
    private static final int SIDE_W = 118;
    private static final int TOP_H = 34;
    private static final int GAP = 4;
    private static final int NAV_H = 20;

    private enum Page {
        GENERAL("General", "⚙"),
        TIERS("Tier lists", "☰"),
        NAMETAGS("Nametags", "✎"),
        HUD("Opponent HUD", "⚔"),
        THEME("Theme", "✦");

        final String title;
        final String icon;

        Page(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }
    }

    /** Remembered while the game runs, so the window reopens where you left it. */
    private static Page lastPage = Page.GENERAL;

    private final @Nullable Screen parent;
    private Page page = lastPage;
    private List<Option> options = List.of();
    private double scroll, scrollTarget;
    private @Nullable Option dragging;
    private TextFieldWidget search;

    // Layout, set in init()
    private int px, py, pw, ph;
    private int vx, vy, vw, vh;

    public QTiersConfigScreen(@Nullable Screen parent) {
        super(Text.literal("QTiers Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 16, 500);
        ph = Math.min(height - 16, 310);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        int cx = px + SIDE_W;
        int cw = pw - SIDE_W;
        vx = cx + 10;
        vy = py + TOP_H + 2;
        vw = cw - 20;
        vh = ph - TOP_H - 10;

        int searchW = Math.min(120, cw - 140);
        search = addDrawableChild(Ui.searchField(textRenderer, searchX(searchW), py + 7, searchW, search));
        addDrawableChild(Ui.button(px + pw - 28, py + 7, 20, 20, "✕", b -> close()));
        options = build(page);
    }

    private int searchX(int searchW) {
        return px + pw - 28 - 6 - searchW;
    }

    // ------------------------------------------------------------------ pages

    private List<Option> build(Page page) {
        QTiersConfig c = QTiersConfig.get();
        List<Option> list = new ArrayList<>();
        switch (page) {
            case GENERAL -> {
                list.add(new Header("Display"));
                list.add(new Toggle("Enabled", "Show tiers at all", () -> c.enabled, v -> c.enabled = v));
                list.add(new Toggle("Nametags", "Tiers next to names above players' heads", () -> c.showInNametags, v -> c.showInNametags = v));
                list.add(new Toggle("Tab list", "Tiers next to names in the player list", () -> c.showInTabList, v -> c.showInTabList = v));
                list.add(new Toggle("Own name in F5", "See your own nametag in third person", () -> c.showOwnNametag, v -> c.showOwnNametag = v));
                list.add(new Toggle("Clickable chat names", "Click a name in chat to open that player's profile", () -> c.clickableNames, v -> c.clickableNames = v));
                list.add(new Header("Data"));
                list.add(new Action("Refresh tiers", "Fetch every player's tiers again", "Refresh", TierApi::clearCache));
                list.add(new Action("Online players", "Everyone on this server with their tiers", "Open",
                        () -> client.setScreen(new QTiersLobbyScreen(this))));
                list.add(new Action("Leaderboards", "Top players of each tier list", "Open",
                        () -> client.setScreen(new QTiersLeaderboardScreen(this))));
            }
            case TIERS -> {
                list.add(new Header("Position and gamemode"));
                for (TierSource source : TierSource.values()) list.add(new SiteRow(source));
                list.add(new Header("Behaviour"));
                list.add(new Choice<>("Show highest tier", "When to show a player's best tier instead",
                        Arrays.asList(HighestMode.values()), () -> c.highestMode, v -> c.highestMode = v,
                        m -> Text.literal(switch (m) {
                            case NEVER -> "Never";
                            case NOT_FOUND -> "If not ranked";
                            case ALWAYS -> "Always";
                        })));
                list.add(new Toggle("Show retired", "Include retired tiers, shown as RHT1 etc.", () -> c.showRetired, v -> c.showRetired = v));
            }
            case NAMETAGS -> {
                list.add(new NametagPreview());
                list.add(new Header("Content"));
                list.add(new Toggle("Gamemode icons", "Each site's icon in front of the tier", () -> c.showIcons, v -> c.showIcons = v));
                list.add(new Toggle("Site labels", "MCT / PVP / SUB / MC / HQ / TMC before each tier", () -> c.showSiteLabel, v -> c.showSiteLabel = v));
                list.add(new Toggle("Bold tiers", "Draw tier labels in bold", () -> c.boldTiers, v -> c.boldTiers = v));
                list.add(new Toggle("Brackets", "[HT3] instead of HT3", () -> c.tierBrackets, v -> c.tierBrackets = v));
                list.add(new Header("Separator"));
                list.add(new Choice<>("Separator", "Between the name and the tiers", QTiersConfig.SEPARATORS,
                        () -> c.separator, v -> c.separator = v, s -> Text.literal(s.isEmpty() ? "None" : s)));
                list.add(new Toggle("Colored separator", "Separator takes the tier's color", () -> c.coloredSeparator, v -> c.coloredSeparator = v));
                list.add(new Header("Position"));
                list.add(new Slider("Nametag height", "How far tags are raised above the head", 0, 1, 0.05,
                        () -> c.nametagHeight, v -> c.nametagHeight = v, v -> String.format("+%.2f", v)));
            }
            case HUD -> {
                list.add(new HudPreview());
                list.add(new Header("Opponent HUD"));
                list.add(new Toggle("Enabled", "Shows the tiers of the last player you hit", () -> c.opponentHud, v -> c.opponentHud = v));
                list.add(new Action("Position", "Drag the HUD anywhere on the screen", "Move",
                        () -> client.setScreen(new QTiersHudEditScreen(this))));
                list.add(new Slider("Scale", "Size of the HUD", 0.5, 2, 0.05,
                        () -> c.hudScale, v -> c.hudScale = v, v -> String.format("%.2fx", v)));
                list.add(new Header("Style"));
                list.add(new Colors("Background", null, Theme.HUD_BACKGROUNDS, () -> c.hudBackground, v -> c.hudBackground = v));
                list.add(new Slider("Background opacity", null, 0, 100, 5,
                        () -> c.hudOpacity, v -> c.hudOpacity = (int) Math.round(v), v -> Math.round(v) + "%"));
                list.add(new Toggle("Border", "Outline in the accent color", () -> c.hudBorder, v -> c.hudBorder = v));
                list.add(new Toggle("Accent title", "Player name in the accent color", () -> c.hudAccentTitle, v -> c.hudAccentTitle = v));
                list.add(new Toggle("Text shadow", null, () -> c.hudShadow, v -> c.hudShadow = v));
                list.add(new Toggle("Gamemode names", "\"Sword\", \"Mace\"... after each tier", () -> c.hudModeNames, v -> c.hudModeNames = v));
                list.add(new Action("Reset style", "Back to the default look", "Reset", () -> {
                    c.hudScale = 1;
                    c.hudBackground = 0;
                    c.hudOpacity = 56;
                    c.hudBorder = false;
                    c.hudAccentTitle = false;
                    c.hudShadow = true;
                    c.hudModeNames = true;
                }));
            }
            case THEME -> {
                list.add(new Header("Appearance"));
                list.add(new Choice<>("Style", "Colors of the QTiers screens", Arrays.asList(UiStyle.values()),
                        () -> c.uiStyle, v -> {
                            c.uiStyle = v;
                            rebuildLater = true;
                        },
                        s -> Text.literal(switch (s) {
                            case DARK -> "Dark";
                            case MIDNIGHT -> "Midnight";
                            case LIGHT -> "Light";
                        })));
                list.add(new Colors("Accent color", "Switches, sliders and highlights", Theme.ACCENTS,
                        () -> c.accentColor, v -> c.accentColor = v));
                list.add(new Slider("Corner roundness", "Panels, buttons and the opponent HUD", 0, 6, 1,
                        () -> c.cornerRadius, v -> c.cornerRadius = (int) Math.round(v), v -> String.valueOf(Math.round(v))));
                list.add(new Action("Reset theme", "Dark style, blue accent", "Reset", () -> {
                    c.uiStyle = UiStyle.DARK;
                    c.accentColor = 0x4F8CFF;
                    c.cornerRadius = 4;
                    rebuildLater = true;
                }));
            }
        }
        return list;
    }

    /** Set when a change needs the widgets rebuilt (e.g. the search box colors after a style change). */
    private boolean rebuildLater;

    private void switchPage(Page next) {
        page = next;
        lastPage = next;
        scroll = scrollTarget = 0;
        dragging = null;
        options = build(next);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        Theme.Palette p = Theme.palette();
        int r = Theme.radius() + 2;

        // Window with drop shadow
        Ui.rect(c, px - 2, py + 1, pw + 4, ph + 4, r + 2, 0x30000000);
        Ui.rect(c, px, py, pw, ph, r, p.panel());
        // Sidebar: rounded on the left, square where it meets the content
        Ui.rect(c, px, py, SIDE_W, ph, r, p.sidebar());
        c.fill(px + SIDE_W - r, py, px + SIDE_W, py + ph, p.sidebar());
        c.fill(px + SIDE_W, py, px + SIDE_W + 1, py + ph, p.border());
        Ui.outline(c, px, py, pw, ph, r, p.border());

        renderSidebar(c, mouseX, mouseY);

        // Top bar of the content side
        Ui.text(c, Text.literal(page.title).formatted(Formatting.BOLD), px + SIDE_W + 12, py + 13, p.text());
        c.fill(px + SIDE_W + 1, py + TOP_H - 1, px + pw - 1, py + TOP_H, p.border());
        int searchW = Math.min(120, pw - SIDE_W - 140);
        Ui.searchBox(c, search, searchX(searchW), py + 7, searchW);

        renderOptions(c, mouseX, mouseY);

        super.render(c, mouseX, mouseY, delta);
        if (rebuildLater) {
            rebuildLater = false;
            clearAndInit();
        }
    }

    private void renderSidebar(DrawContext c, int mouseX, int mouseY) {
        Theme.Palette p = Theme.palette();
        c.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, px + 10, py + 8, 0, 0, 20, 20, 20, 20);
        Ui.text(c, Text.literal("QTiers").formatted(Formatting.BOLD), px + 35, py + 9, p.text());
        Ui.text(c, "v" + version(), px + 35, py + 19, p.textMuted());

        int y = py + 40;
        for (Page pg : Page.values()) {
            navItem(c, pg.icon, pg.title, y, pg == page, mouseX, mouseY);
            y += NAV_H + 2;
        }

        // Shortcuts to the other QTiers screens at the bottom
        int linksY = py + ph - 8 - 2 * (NAV_H + 2);
        c.fill(px + 10, linksY - 6, px + SIDE_W - 10, linksY - 5, p.border());
        navItem(c, "☺", "Online players", linksY, false, mouseX, mouseY);
        navItem(c, "♛", "Leaderboards", linksY + NAV_H + 2, false, mouseX, mouseY);
    }

    private void navItem(DrawContext c, String icon, String label, int y, boolean selected, int mouseX, int mouseY) {
        Theme.Palette p = Theme.palette();
        int x = px + 6, w = SIDE_W - 12;
        boolean hovered = in(mouseX, mouseY, x, y, w, NAV_H);
        if (selected) {
            Ui.rect(c, x, y, w, NAV_H, Theme.radius(), Theme.alpha(Theme.accent(), 0x38));
            Ui.rect(c, x, y + 5, 2, NAV_H - 10, 1, Theme.accent());
        } else if (hovered) {
            Ui.rect(c, x, y, w, NAV_H, Theme.radius(), p.cardHover());
        }
        int color = selected ? Theme.mix(Theme.accent(), p.text(), Theme.light() ? 0.2f : 0.35f) : hovered ? p.text() : p.textDim();
        Ui.text(c, icon, x + 8, y + 6, color);
        Ui.text(c, label, x + 22, y + 6, color);
    }

    private static String version() {
        return FabricLoader.getInstance().getModContainer(QTiers.MOD_ID)
                .map(m -> m.getMetadata().getVersion().getFriendlyString().replaceAll("\\+.*", ""))
                .orElse("?");
    }

    private int contentHeight() {
        int h = 0;
        for (Option o : options) h += o.height() + GAP;
        return Math.max(0, h - GAP);
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - vh);
    }

    private int optionWidth() {
        return maxScroll() > 0 ? vw - 6 : vw;
    }

    private void renderOptions(DrawContext c, int mouseX, int mouseY) {
        scrollTarget = Math.max(0, Math.min(scrollTarget, maxScroll()));
        scroll += (scrollTarget - scroll) * 0.4;
        if (Math.abs(scrollTarget - scroll) < 0.5) scroll = scrollTarget;

        int w = optionWidth();
        boolean mouseInView = in(mouseX, mouseY, vx, vy, vw, vh);
        c.enableScissor(vx, vy, vx + vw, vy + vh);
        int y = vy - (int) Math.round(scroll);
        for (Option o : options) {
            int h = o.height();
            if (y + h >= vy && y <= vy + vh) {
                boolean hovered = mouseInView && in(mouseX, mouseY, vx, y, w, h);
                o.render(c, vx, y, w, h, mouseX, mouseY, hovered);
            }
            y += h + GAP;
        }
        c.disableScissor();

        if (maxScroll() > 0) {
            Theme.Palette p = Theme.palette();
            int total = contentHeight();
            int thumb = Math.max(16, vh * vh / total);
            int thumbY = vy + (int) ((vh - thumb) * scroll / maxScroll());
            Ui.rect(c, vx + vw - 3, vy, 3, vh, 1, Theme.alpha(p.border(), 0x80));
            Ui.rect(c, vx + vw - 3, thumbY, 3, thumb, 1, p.textMuted());
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        setFocused(null);
        double mx = click.x(), my = click.y();

        int y = py + 40;
        for (Page pg : Page.values()) {
            if (in(mx, my, px + 6, y, SIDE_W - 12, NAV_H)) {
                if (pg != page) switchPage(pg);
                return true;
            }
            y += NAV_H + 2;
        }
        int linksY = py + ph - 8 - 2 * (NAV_H + 2);
        if (in(mx, my, px + 6, linksY, SIDE_W - 12, NAV_H)) {
            client.setScreen(new QTiersLobbyScreen(this));
            return true;
        }
        if (in(mx, my, px + 6, linksY + NAV_H + 2, SIDE_W - 12, NAV_H)) {
            client.setScreen(new QTiersLeaderboardScreen(this));
            return true;
        }

        if (!in(mx, my, vx, vy, vw, vh)) return false;
        int w = optionWidth();
        int oy = vy - (int) Math.round(scroll);
        for (Option o : List.copyOf(options)) {
            int h = o.height();
            if (my >= oy && my < oy + h && mx >= vx && mx < vx + w) {
                if (o.click(mx, my, click.button(), vx, oy, w, h)) {
                    QTiersConfig.save();
                    return true;
                }
                return false;
            }
            oy += h + GAP;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) {
            dragging.drag(click.x());
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            QTiersConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollTarget = Math.max(0, Math.min(scrollTarget - verticalAmount * 28, maxScroll()));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (QTiersProfileScreen.searchEntered(search, input)) {
            QTiersConfig.save();
            client.setScreen(new QTiersProfileScreen(search.getText(), this));
            return true;
        }
        return super.keyPressed(input);
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public void close() {
        QTiersConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    // ------------------------------------------------------------------ options

    /** One row in the content area. */
    private abstract static class Option {
        final String label;
        final @Nullable String description;

        Option(String label, @Nullable String description) {
            this.label = label;
            this.description = description;
        }

        int height() {
            return description == null ? 24 : 32;
        }

        void render(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {
            Theme.Palette p = Theme.palette();
            Ui.rect(c, x, y, w, h, Theme.radius(), hovered ? p.cardHover() : p.card());
            if (description == null) {
                Ui.text(c, label, x + 10, y + (h - 8) / 2, p.text());
            } else {
                Ui.text(c, label, x + 10, y + 7, p.text());
                Ui.clipped(c, Text.literal(description), x + 10, y + 18, w - 20 - controlWidth(), p.textMuted());
            }
            renderControl(c, x, y, w, h, mx, my, hovered);
        }

        /** Space the control takes on the right, so the description doesn't run under it. */
        int controlWidth() {
            return 0;
        }

        void renderControl(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {}

        boolean click(double mx, double my, int button, int x, int y, int w, int h) {
            return false;
        }

        void drag(double mx) {}
    }

    private static final class Header extends Option {
        Header(String label) {
            super(label, null);
        }

        @Override
        int height() {
            return 14;
        }

        @Override
        void render(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {
            Ui.text(c, Text.literal(label.toUpperCase()).formatted(Formatting.BOLD), x + 2, y + 5, Theme.accent());
        }
    }

    private static final class Toggle extends Option {
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private float anim;

        Toggle(String label, @Nullable String description, BooleanSupplier get, Consumer<Boolean> set) {
            super(label, description);
            this.get = get;
            this.set = set;
            this.anim = get.getAsBoolean() ? 1 : 0;
        }

        @Override
        int controlWidth() {
            return 34;
        }

        @Override
        void renderControl(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {
            float target = get.getAsBoolean() ? 1 : 0;
            anim += (target - anim) * 0.35f;
            if (Math.abs(target - anim) < 0.01f) anim = target;
            Ui.toggle(c, x + w - 32, y + (h - 12) / 2, anim, hovered);
        }

        @Override
        boolean click(double mx, double my, int button, int x, int y, int w, int h) {
            if (button != 0) return false;
            set.accept(!get.getAsBoolean());
            return true;
        }
    }

    /** A value picked with ‹ › arrows: left half goes back, right half forward. */
    private static final class ChoiceBox<T> {
        private final List<T> values;
        private final Supplier<T> get;
        private final Consumer<T> set;
        private final Function<T, Text> name;

        ChoiceBox(List<T> values, Supplier<T> get, Consumer<T> set, Function<T, Text> name) {
            this.values = values;
            this.get = get;
            this.set = set;
            this.name = name;
        }

        void render(DrawContext c, int x, int y, int w, int mx, int my) {
            Theme.Palette p = Theme.palette();
            boolean hovered = in(mx, my, x, y, w, 16);
            Ui.rect(c, x, y, w, 16, Theme.radius(), hovered ? Theme.mix(p.control(), p.text(), 0.1f) : p.control());
            boolean left = hovered && mx < x + w / 2.0;
            Ui.text(c, "‹", x + 5, y + 4, hovered && left ? Theme.accent() : p.textDim());
            Ui.text(c, "›", x + w - 9, y + 4, hovered && !left ? Theme.accent() : p.textDim());
            Text value = name.apply(get.get());
            var tr = MinecraftClient.getInstance().textRenderer;
            int tw = Math.min(tr.getWidth(value), w - 26);
            Ui.clipped(c, value, x + (w - tw) / 2, y + 4, w - 26, p.text());
        }

        boolean click(double mx, double my, int button, int x, int y, int w) {
            if (!in(mx, my, x, y, w, 16)) return false;
            int i = Math.max(0, values.indexOf(get.get()));
            boolean back = button == 1 || mx < x + w / 2.0;
            set.accept(values.get(Math.floorMod(i + (back ? -1 : 1), values.size())));
            return true;
        }
    }

    private static final class Choice<T> extends Option {
        private static final int BOX_W = 112;
        private final ChoiceBox<T> box;

        Choice(String label, @Nullable String description, List<T> values, Supplier<T> get, Consumer<T> set, Function<T, Text> name) {
            super(label, description);
            this.box = new ChoiceBox<>(values, get, set, name);
        }

        @Override
        int controlWidth() {
            return BOX_W + 10;
        }

        @Override
        void renderControl(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {
            box.render(c, x + w - 10 - BOX_W, y + (h - 16) / 2, BOX_W, mx, my);
        }

        @Override
        boolean click(double mx, double my, int button, int x, int y, int w, int h) {
            return box.click(mx, my, button, x + w - 10 - BOX_W, y + (h - 16) / 2, BOX_W);
        }
    }

    /** One tier list: its position and which gamemode it shows, side by side. */
    private static final class SiteRow extends Option {
        private static final int POS_W = 64, MODE_W = 112;
        private final TierSource source;
        private final ChoiceBox<Position> position;
        private final ChoiceBox<String> mode;

        SiteRow(TierSource source) {
            super(source.displayName, null);
            this.source = source;
            QTiersConfig.SiteSettings site = QTiersConfig.get().site(source);
            this.position = new ChoiceBox<>(Arrays.asList(Position.values()), () -> site.position, v -> site.position = v,
                    v -> switch (v) {
                        case LEFT -> Text.literal("Left");
                        case RIGHT -> Text.literal("Right");
                        case OFF -> Text.literal("Off").styled(s -> s.withColor(0xEF4444));
                    });
            this.mode = new ChoiceBox<>(GameModes.cycleFor(source), () -> site.gamemode, v -> site.gamemode = v,
                    v -> ModeTexts.styledName(source, v));
        }

        @Override
        int height() {
            return 26;
        }

        @Override
        void render(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {
            Theme.Palette p = Theme.palette();
            Ui.rect(c, x, y, w, h, Theme.radius(), hovered ? p.cardHover() : p.card());
            Ui.rect(c, x + 6, y + 8, 3, h - 16, 1, 0xFF000000 | source.color);
            Ui.text(c, Text.literal(source.displayName).formatted(Formatting.BOLD), x + 14, y + (h - 8) / 2, p.text());
            mode.render(c, x + w - 10 - MODE_W, y + 5, MODE_W, mx, my);
            position.render(c, x + w - 10 - MODE_W - 4 - POS_W, y + 5, POS_W, mx, my);
        }

        @Override
        boolean click(double mx, double my, int button, int x, int y, int w, int h) {
            return mode.click(mx, my, button, x + w - 10 - MODE_W, y + 5, MODE_W)
                    || position.click(mx, my, button, x + w - 10 - MODE_W - 4 - POS_W, y + 5, POS_W);
        }
    }

    private final class Slider extends Option {
        private static final int TRACK_W = 100;
        private final double min, max, step;
        private final DoubleSupplier get;
        private final DoubleConsumer set;
        private final Function<Double, String> format;
        private int trackX;

        Slider(String label, @Nullable String description, double min, double max, double step,
               DoubleSupplier get, DoubleConsumer set, Function<Double, String> format) {
            super(label, description);
            this.min = min;
            this.max = max;
            this.step = step;
            this.get = get;
            this.set = set;
            this.format = format;
        }

        @Override
        int controlWidth() {
            return TRACK_W + 50;
        }

        @Override
        void renderControl(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {
            trackX = x + w - 12 - TRACK_W;
            double value = get.getAsDouble();
            Ui.slider(c, trackX, y + (h - 12) / 2, TRACK_W, (value - min) / (max - min), dragging == this);
            String text = format.apply(value);
            Ui.text(c, text, trackX - 8 - textRenderer.getWidth(text), y + (h - 8) / 2, Theme.palette().textDim());
        }

        @Override
        boolean click(double mx, double my, int button, int x, int y, int w, int h) {
            trackX = x + w - 12 - TRACK_W;
            if (button != 0 || mx < trackX - 6 || mx > trackX + TRACK_W + 6) return false;
            dragging = this;
            drag(mx);
            return true;
        }

        @Override
        void drag(double mx) {
            double f = Math.max(0, Math.min(1, (mx - trackX) / TRACK_W));
            double v = min + Math.round((max - min) * f / step) * step;
            set.accept(Math.max(min, Math.min(max, v)));
        }
    }

    private static final class Colors extends Option {
        private static final int SIZE = 12, SPACING = 4;
        private final int[] presets;
        private final IntSupplier get;
        private final IntConsumer set;

        Colors(String label, @Nullable String description, int[] presets, IntSupplier get, IntConsumer set) {
            super(label, description);
            this.presets = presets;
            this.get = get;
            this.set = set;
        }

        @Override
        int controlWidth() {
            return presets.length * (SIZE + SPACING) + 10;
        }

        private int startX(int x, int w) {
            return x + w - 10 - presets.length * (SIZE + SPACING) + SPACING;
        }

        @Override
        void renderControl(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {
            Theme.Palette p = Theme.palette();
            int sx = startX(x, w);
            int sy = y + (h - SIZE) / 2;
            for (int i = 0; i < presets.length; i++) {
                int cx = sx + i * (SIZE + SPACING);
                boolean selected = presets[i] == get.getAsInt();
                if (selected) Ui.outline(c, cx - 2, sy - 2, SIZE + 4, SIZE + 4, SIZE / 2 + 2, p.text());
                Ui.rect(c, cx, sy, SIZE, SIZE, SIZE / 2, 0xFF000000 | presets[i]);
                if (in(mx, my, cx, sy, SIZE, SIZE) && !selected) Ui.outline(c, cx - 1, sy - 1, SIZE + 2, SIZE + 2, SIZE / 2 + 1, p.textMuted());
                else Ui.outline(c, cx, sy, SIZE, SIZE, SIZE / 2, 0x30000000);
            }
        }

        @Override
        boolean click(double mx, double my, int button, int x, int y, int w, int h) {
            int sx = startX(x, w);
            int sy = y + (h - SIZE) / 2;
            for (int i = 0; i < presets.length; i++) {
                if (in(mx, my, sx + i * (SIZE + SPACING) - 2, sy - 2, SIZE + 4, SIZE + 4)) {
                    set.accept(presets[i]);
                    return true;
                }
            }
            return false;
        }
    }

    private static final class Action extends Option {
        private static final int BUTTON_W = 64;
        private final String button;
        private final Runnable action;

        Action(String label, @Nullable String description, String button, Runnable action) {
            super(label, description);
            this.button = button;
            this.action = action;
        }

        @Override
        int controlWidth() {
            return BUTTON_W + 10;
        }

        @Override
        void renderControl(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {
            int bx = x + w - 10 - BUTTON_W, by = y + (h - 16) / 2;
            Ui.button(c, bx, by, BUTTON_W, 16, Text.literal(button), in(mx, my, bx, by, BUTTON_W, 16), true, true);
        }

        @Override
        boolean click(double mx, double my, int b, int x, int y, int w, int h) {
            if (b != 0 || !in(mx, my, x + w - 10 - BUTTON_W, y + (h - 16) / 2, BUTTON_W, 16)) return false;
            action.run();
            return true;
        }
    }

    /** Your own name as it shows above your head, with the current nametag settings. */
    private final class NametagPreview extends Option {
        NametagPreview() {
            super("Preview", null);
        }

        @Override
        int height() {
            return 40;
        }

        @Override
        void render(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {
            Ui.rect(c, x, y, w, h, Theme.radius(), 0xFF2B3A4A);
            Ui.rect(c, x, y + h / 2, w, h - h / 2, Theme.radius(), 0xFF3C5A2E);
            c.fill(x, y + h / 2, x + w, y + h / 2 + Math.min(Theme.radius(), h / 2), 0xFF3C5A2E);
            Text name = TierDisplay.decorate(selfId(), Text.literal(selfName()));
            int tw = textRenderer.getWidth(name);
            int tx = x + (w - tw) / 2, ty = y + (h - 8) / 2;
            c.fill(tx - 2, ty - 1, tx + tw + 1, ty + 9, 0x40000000);
            c.drawText(textRenderer, name, tx, ty, 0xFFFFFFFF, false);
        }
    }

    /** The opponent HUD with your own tiers, in its current style. */
    private final class HudPreview extends Option {
        HudPreview() {
            super("Preview", null);
        }

        @Override
        int height() {
            OpponentHud.Box box = OpponentHud.build(selfId(), selfName());
            return Math.max(40, OpponentHud.scaledHeight(box) + 16);
        }

        @Override
        void render(DrawContext c, int x, int y, int w, int h, int mx, int my, boolean hovered) {
            Theme.Palette p = Theme.palette();
            Ui.rect(c, x, y, w, h, Theme.radius(), p.card());
            // Light checker so the HUD's transparency shows
            c.enableScissor(Math.max(x, vx), Math.max(y, vy), Math.min(x + w, vx + vw), Math.min(y + h, vy + vh));
            for (int cy = y + 4, row = 0; cy < y + h - 4; cy += 8, row++) {
                for (int cx = x + 4 + (row % 2) * 8; cx < x + w - 4; cx += 16) {
                    c.fill(cx, cy, Math.min(cx + 8, x + w - 4), Math.min(cy + 8, y + h - 4), Theme.alpha(p.text(), 0x0C));
                }
            }
            OpponentHud.Box box = OpponentHud.build(selfId(), selfName());
            OpponentHud.draw(c, box, x + (w - OpponentHud.scaledWidth(box)) / 2, y + (h - OpponentHud.scaledHeight(box)) / 2);
            c.disableScissor();
        }
    }

    private UUID selfId() {
        if (client.player != null) return client.player.getUuid();
        UUID id = client.getSession().getUuidOrNull();
        return id != null ? id : new UUID(0, 0);
    }

    private String selfName() {
        return client.player != null ? client.player.getGameProfile().name() : client.getSession().getUsername();
    }
}
