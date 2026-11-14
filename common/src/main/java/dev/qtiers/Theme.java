package dev.qtiers;

/** Colors of the QTiers screens, from the chosen style and accent. All colors are ARGB. */
public final class Theme {
    private Theme() {}

    public record Palette(int panel, int sidebar, int card, int cardHover, int border, int control,
                          int text, int textDim, int textMuted) {}

    private static final Palette DARK = new Palette(
            0xF2141519, 0xF20E0F12, 0xFF1C1E23, 0xFF24272D, 0xFF2C2F36, 0xFF34373F,
            0xFFF2F3F5, 0xFFA3A7B0, 0xFF6B7079);
    private static final Palette MIDNIGHT = new Palette(
            0xF20A0E19, 0xF2070A12, 0xFF121828, 0xFF182034, 0xFF222B40, 0xFF2A3550,
            0xFFE8ECF5, 0xFF9AA3B8, 0xFF5E6880);
    private static final Palette LIGHT = new Palette(
            0xF5F3F4F6, 0xF5E6E8EC, 0xFFFFFFFF, 0xFFEEF0F3, 0xFFD5D8DE, 0xFFC9CDD4,
            0xFF16181D, 0xFF505662, 0xFF8A909B);

    /** Accent presets offered in the settings. */
    public static final int[] ACCENTS = {
            0x4F8CFF, 0x8B5CF6, 0xEC4899, 0xEF4444, 0xF97316,
            0xE8BA3A, 0x22C55E, 0x14B8A6, 0x06B6D4, 0xE5E7EB};

    /** Background presets for the opponent HUD. */
    public static final int[] HUD_BACKGROUNDS = {
            0x000000, 0x0F172A, 0x1C1E23, 0x1E1B4B, 0x3B0764, 0x450A0A, 0x052E16, 0xFFFFFF};

    public static Palette palette() {
        return switch (QTiersConfig.get().uiStyle) {
            case MIDNIGHT -> MIDNIGHT;
            case LIGHT -> LIGHT;
            default -> DARK;
        };
    }

    public static boolean light() {
        return QTiersConfig.get().uiStyle == QTiersConfig.UiStyle.LIGHT;
    }

    public static int accent() {
        return 0xFF000000 | QTiersConfig.get().accentColor;
    }

    public static int radius() {
        return QTiersConfig.get().cornerRadius;
    }

    /** Text color that reads well on top of the accent. */
    public static int onAccent() {
        return luminance(accent()) > 0.6 ? 0xFF111111 : 0xFFFFFFFF;
    }

    public static int alpha(int color, int alpha) {
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    /** Blends two ARGB colors; t = 0 gives a, t = 1 gives b. */
    public static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF, cb = (b >>> shift) & 0xFF;
            r |= Math.round(ca + (cb - ca) * t) << shift;
        }
        return r;
    }

    public static double luminance(int color) {
        return (0.299 * ((color >> 16) & 0xFF) + 0.587 * ((color >> 8) & 0xFF) + 0.114 * (color & 0xFF)) / 255.0;
    }
}
