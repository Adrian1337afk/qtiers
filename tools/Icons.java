import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Original QTiers gamemode icons. Drawn in a 32x32 coordinate space at 4x resolution,
 * given a 1px dark outline, then box-downscaled to 32x32.
 * Usage: java tools/Icons.java src/main/resources/assets/qtiers/textures/icons/qtiers
 */
public class Icons {
    static final int S = 4;          // supersampling factor
    static final int N = 32 * S;     // working resolution
    static Graphics2D g;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        Map<String, Runnable> icons = new LinkedHashMap<>();
        icons.put("axe", Icons::axe);
        icons.put("mace", Icons::mace);
        icons.put("nethop", Icons::nethop);
        icons.put("pot", Icons::pot);
        icons.put("smp", Icons::smp);
        icons.put("sword", Icons::sword);
        icons.put("uhc", Icons::uhc);
        icons.put("vanilla", Icons::vanilla);
        icons.put("bed", Icons::bed);
        icons.put("bow", Icons::bow);
        icons.put("creeper", Icons::creeper);
        icons.put("debuff", Icons::debuff);
        icons.put("dia_crystal", Icons::diaCrystal);
        icons.put("dia_smp", Icons::diaSmp);
        icons.put("elytra", Icons::elytra);
        icons.put("manhunt", Icons::manhunt);
        icons.put("minecart", Icons::minecart);
        icons.put("og_vanilla", Icons::ogVanilla);
        icons.put("speed", Icons::speed);
        icons.put("trident", Icons::trident);

        for (Map.Entry<String, Runnable> e : icons.entrySet()) {
            BufferedImage art = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
            g = art.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.scale(S, S);
            e.getValue().run();
            g.dispose();
            File f = new File(out, e.getKey() + ".png");
            f.getParentFile().mkdirs();
            ImageIO.write(downscale(outline(art)), "png", f);
        }
    }

    // ---------- drawing helpers (32x32 coordinates) ----------

    static Color c(int rgb) { return new Color(rgb); }
    static Color c(int rgb, int alpha) { return new Color((alpha << 24) | rgb, true); }

    static void fill(Shape s, int rgb) { g.setColor(c(rgb)); g.fill(s); }
    static void fill(Shape s, Paint p) { g.setPaint(p); g.fill(s); }

    static void line(double x1, double y1, double x2, double y2, double w, int rgb) {
        g.setColor(c(rgb));
        g.setStroke(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    static void stroke(Shape s, double w, int rgb) {
        g.setColor(c(rgb));
        g.setStroke(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(s);
    }

    static Path2D poly(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.closePath();
        return p;
    }

    static Ellipse2D circle(double cx, double cy, double r) { return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2); }
    static Shape rect(double x1, double y1, double x2, double y2) { return new Rectangle2D.Double(x1, y1, x2 - x1, y2 - y1); }
    static Shape round(double x1, double y1, double x2, double y2, double r) { return new RoundRectangle2D.Double(x1, y1, x2 - x1, y2 - y1, r * 2, r * 2); }

    static void shine(double cx, double cy, double rx, double ry) {
        g.setColor(c(0xFFFFFF, 170));
        g.fill(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
    }

    static void clipped(Shape clip, Runnable r) {
        Shape old = g.getClip();
        g.clip(clip);
        r.run();
        g.setClip(old);
    }

    // ---------- icons ----------

    static void sword() {
        // diamond blade with a centre ridge, gold guard, leather grip
        fill(poly(10.2, 19.4, 24.6, 5.0, 28.5, 3.5, 27.0, 7.4, 12.6, 21.8), 0x58D3E6);
        line(11.4, 20.6, 26.4, 5.6, 1.1, 0xB8F4FF);
        line(7.6, 17.6, 14.4, 24.4, 3.0, 0xE0A93A);
        line(10.8, 21.2, 6.0, 26.0, 2.8, 0x6B4426);
        fill(circle(5.0, 27.0, 2.1), 0xE0A93A);
    }

    static void axe() {
        line(6.0, 27.0, 24.0, 9.0, 3.0, 0x9A6A3C);
        // broad blade on the upper-left side of the handle, curved cutting edge
        Path2D head = new Path2D.Double();
        head.moveTo(18.0, 15.0);
        head.lineTo(24.0, 9.0);
        head.lineTo(21.5, 2.5);
        head.quadTo(11.0, 3.0, 10.5, 13.5);
        head.closePath();
        fill(head, 0xA9B4C2);
        clipped(head, () -> {
            Path2D edge = new Path2D.Double();
            edge.moveTo(21.5, 2.5);
            edge.quadTo(11.0, 3.0, 10.5, 13.5);
            stroke(edge, 3.4, 0xE4EBF3);
            fill(poly(18.0, 15.0, 24.0, 9.0, 26, 12, 20, 18), 0x7D8896);
        });
        line(24.0, 9.0, 26.5, 6.5, 3.0, 0x7A5230);
    }

    static void mace() {
        line(6.0, 27.0, 17.0, 16.0, 3.0, 0x7A5A3A);
        line(5.6, 27.4, 8.2, 24.8, 3.4, 0x4E3A26);
        double cx = 20.5, cy = 11.5;
        // spikes at the four non-handle diagonals/axes
        for (double a : new double[]{-90, -45, 0, 180, -135}) {
            double r = Math.toRadians(a);
            double ox = Math.cos(r), oy = Math.sin(r), px = -oy, py = ox;
            fill(poly(cx + ox * 5 + px * 2.0, cy + oy * 5 + py * 2.0,
                    cx + ox * 9.6, cy + oy * 9.6,
                    cx + ox * 5 - px * 2.0, cy + oy * 5 - py * 2.0), 0xC7CCD5);
        }
        fill(circle(cx, cy, 6.2), new RadialGradientPaint(new Point2D.Double(18.5, 9.5), 7.5f,
                new float[]{0f, 1f}, new Color[]{c(0xB9BFC9), c(0x5D626D)}));
        shine(18.4, 9.2, 1.6, 1.1);
    }

    static void nethop() {
        // netherite helmet, front view
        Path2D helm = new Path2D.Double();
        helm.moveTo(4.5, 27);
        helm.lineTo(4.5, 15);
        helm.curveTo(4.5, 7.5, 9.5, 4, 16, 4);
        helm.curveTo(22.5, 4, 27.5, 7.5, 27.5, 15);
        helm.lineTo(27.5, 27);
        helm.lineTo(21.5, 27);
        helm.lineTo(21.5, 18.5);
        helm.lineTo(10.5, 18.5);
        helm.lineTo(10.5, 27);
        helm.closePath();
        fill(helm, 0x4A4249);
        clipped(helm, () -> {
            fill(rect(0, 13.5, 32, 16), 0x2F2A2F);
            fill(rect(0, 0, 32, 9.5), 0x625862);
        });
        shine(10.5, 8.2, 2.4, 1.3);
    }

    static void pot() {
        // round splash-style potion with a cork
        Shape body = circle(16, 20, 9.2);
        fill(rect(12.8, 5.5, 19.2, 12.5), 0xD6E6F1);
        fill(body, 0xD6E6F1);
        clipped(body, () -> fill(rect(0, 18.5, 32, 32), 0xE23B57));
        clipped(body, () -> fill(rect(0, 18.5, 32, 20.2), 0xF2738A));
        fill(round(12.2, 2.6, 19.8, 6.8, 1.2), 0x9A6534);
        shine(11.8, 15.6, 1.8, 2.6);
    }

    static void smp() {
        // ender pearl
        Shape pearl = circle(16, 16, 11.5);
        fill(pearl, new RadialGradientPaint(new Point2D.Double(12.5, 11.5), 15f,
                new float[]{0f, 0.55f, 1f}, new Color[]{c(0x7FF5D8), c(0x1E9A82), c(0x0A3B34)}));
        fill(circle(16, 16, 5.2), 0x0D4A41);
        fill(circle(16, 16, 2.6), 0x3FE0BC);
        shine(10.5, 10.0, 2.6, 1.6);
    }

    static void uhc() {
        Path2D heart = new Path2D.Double();
        heart.moveTo(16, 28);
        heart.curveTo(4, 20, 2.5, 12.5, 5.5, 8);
        heart.curveTo(8.5, 3.5, 14, 4.5, 16, 9.5);
        heart.curveTo(18, 4.5, 23.5, 3.5, 26.5, 8);
        heart.curveTo(29.5, 12.5, 28, 20, 16, 28);
        heart.closePath();
        fill(heart, 0xE5404F);
        clipped(heart, () -> fill(poly(16, 9.5, 32, 4, 32, 32, 16, 32), 0xC22E3D));
        shine(9.6, 10.4, 2.4, 1.7);
    }

    static void vanilla() {
        // end crystal: two nested frames around a glowing core
        stroke(poly(16, 2.5, 29.5, 16, 16, 29.5, 2.5, 16), 2.4, 0xF1E4FF);
        stroke(rect(8.5, 8.5, 23.5, 23.5), 1.6, 0xB89AD8);
        Path2D core = poly(16, 10, 22, 16, 16, 22, 10, 16);
        fill(core, 0xB54FD6);
        fill(poly(16, 10, 16, 16, 10, 16), 0xE493F7);
        fill(poly(22, 16, 16, 22, 16, 16), 0x8A33A8);
    }

    static void bed() {
        fill(rect(4, 21, 6.5, 27), 0x7A4E2C);
        fill(rect(25.5, 21, 28, 27), 0x7A4E2C);
        fill(rect(3, 18.5, 29, 23), 0xA8703F);
        fill(round(3, 12.5, 23, 19.5, 1.5), 0xCC3340);
        fill(rect(3, 17.2, 23, 19.5), 0x9E2430);
        fill(round(21.5, 11.5, 29, 19, 2.2), 0xF1F1F1);
        fill(rect(21.5, 16.8, 29, 19), 0xC9C9C9);
    }

    static void bow() {
        Path2D limb = new Path2D.Double();
        limb.moveTo(11, 3);
        limb.quadTo(33, 16, 11, 29);
        stroke(limb, 3.0, 0x8B5A2B);
        line(11, 3.4, 11, 28.6, 0.9, 0xEDEDED);
        line(4.5, 16, 26.5, 16, 1.6, 0xC9A26E);
        fill(poly(25.0, 13.2, 30.0, 16, 25.0, 18.8), 0x9EA6B2);
        fill(poly(3.0, 13.4, 7.0, 16, 3.0, 18.6, 5.0, 16), 0xF0F0F0);
    }

    static void creeper() {
        Shape head = round(3.5, 3.5, 28.5, 28.5, 3);
        fill(head, 0x5DBB4A);
        clipped(head, () -> {
            int[][] spots = {{5, 5}, {21, 6}, {6, 21}, {23, 22}, {14, 4}, {25, 14}, {4, 13}};
            for (int[] s : spots) fill(rect(s[0], s[1], s[0] + 3, s[1] + 3), 0x7DD46A);
        });
        int face = 0x1D2A1A;
        fill(rect(8, 9, 13, 14), face);
        fill(rect(19, 9, 24, 14), face);
        fill(rect(13, 14, 19, 20), face);
        fill(rect(10.5, 18, 13, 24), face);
        fill(rect(19, 18, 21.5, 24), face);
    }

    static void debuff() {
        // conical flask with a murky poison
        Path2D flask = new Path2D.Double();
        flask.moveTo(13, 11);
        flask.lineTo(5.5, 25.5);
        flask.quadTo(5, 28, 8, 28);
        flask.lineTo(24, 28);
        flask.quadTo(27, 28, 26.5, 25.5);
        flask.lineTo(19, 11);
        flask.closePath();
        fill(rect(13, 4.5, 19, 12), 0xD3E2DA);
        fill(flask, 0xD3E2DA);
        clipped(flask, () -> fill(rect(0, 18.5, 32, 32), 0x4E8A2E));
        clipped(flask, () -> fill(rect(0, 18.5, 32, 20), 0x7DBA4E));
        fill(circle(13, 24, 1.4), 0x9BD86B);
        fill(circle(19, 22.5, 1.0), 0x9BD86B);
        fill(round(12.2, 2.4, 19.8, 6.4, 1.2), 0x3B3B3B);
    }

    static void diaCrystal() {
        Path2D gem = poly(9, 6, 23, 6, 28.5, 13, 16, 28.5, 3.5, 13);
        fill(gem, 0x3FC6E4);
        fill(poly(9, 6, 23, 6, 28.5, 13, 3.5, 13), 0x9EEEFC);
        fill(poly(9, 6, 13, 13, 3.5, 13), 0xC9F7FF);
        fill(poly(3.5, 13, 13, 13, 16, 28.5), 0x2A9EC0);
        line(13, 13, 16, 6.5, 0.8, 0xE8FDFF);
        line(19, 13, 16, 6.5, 0.8, 0x7FDDF2);
    }

    static void diaSmp() {
        // diamond chestplate
        Path2D chest = new Path2D.Double();
        chest.moveTo(7, 4.5);
        chest.lineTo(12, 4.5);
        chest.quadTo(16, 11.5, 20, 4.5);
        chest.lineTo(25, 4.5);
        chest.lineTo(29, 10);
        chest.lineTo(25, 13.5);
        chest.lineTo(25, 27.5);
        chest.lineTo(7, 27.5);
        chest.lineTo(7, 13.5);
        chest.lineTo(3, 10);
        chest.closePath();
        fill(chest, 0x4CD0E2);
        clipped(chest, () -> {
            fill(rect(0, 0, 10, 32), 0x2EA6BE);
            fill(rect(22, 0, 32, 32), 0x2EA6BE);
            fill(rect(0, 21, 32, 23), 0x2EA6BE);
        });
        line(16, 13, 16, 20, 1.2, 0xBDF6FF);
    }

    static void elytra() {
        for (int side : new int[]{-1, 1}) {
            double m = 16;
            Path2D w = new Path2D.Double();
            w.moveTo(m + side * 1.2, 5);
            w.curveTo(m + side * 9, 4.5, m + side * 13, 9.5, m + side * 12.5, 19);
            w.lineTo(m + side * 11, 28);
            w.lineTo(m + side * 8.5, 23.5);
            w.lineTo(m + side * 6.5, 27.5);
            w.lineTo(m + side * 4.5, 21);
            w.lineTo(m + side * 1.2, 12);
            w.closePath();
            fill(w, 0xA79CC4);
            clipped(w, () -> {
                line(m + side * 4, 9, m + side * 7.5, 22, 1.0, 0x7C7199);
                line(m + side * 8, 8, m + side * 10, 22, 1.0, 0x7C7199);
            });
        }
    }

    static void manhunt() {
        // compass with a tilted needle
        fill(circle(16, 16, 12.3), 0x80858E);
        fill(circle(16, 16, 9.6), 0xECE6D6);
        AffineTransform old = g.getTransform();
        g.rotate(Math.toRadians(35), 16, 16);
        fill(poly(16, 7.4, 18.4, 16, 13.6, 16), 0xE0383A);
        fill(poly(16, 24.6, 18.4, 16, 13.6, 16), 0x50555E);
        g.setTransform(old);
        fill(circle(16, 16, 1.5), 0x2A2D33);
    }

    static void minecart() {
        // TNT peeking out of the cart
        fill(rect(9, 4.5, 23, 12), 0xD24A3A);
        fill(rect(9, 7, 23, 9.2), 0xF2F2F2);
        fill(poly(3.5, 11, 28.5, 11, 26, 22.5, 6, 22.5), 0x8C9198);
        fill(rect(3.5, 11, 28.5, 13.6), 0x5E636B);
        line(8, 16.5, 24, 16.5, 1.0, 0x6E737B);
        for (double x : new double[]{10, 22}) {
            fill(circle(x, 24.5, 3.4), 0x34373C);
            fill(circle(x, 24.5, 1.2), 0x9AA0A8);
        }
    }

    static void ogVanilla() {
        // golden apple
        Path2D apple = new Path2D.Double();
        apple.moveTo(16, 10);
        apple.curveTo(11, 6.5, 3.5, 9, 4.5, 17.5);
        apple.curveTo(5.5, 25, 10.5, 29.5, 16, 27);
        apple.curveTo(21.5, 29.5, 26.5, 25, 27.5, 17.5);
        apple.curveTo(28.5, 9, 21, 6.5, 16, 10);
        apple.closePath();
        fill(apple, 0xF2C238);
        clipped(apple, () -> fill(circle(26, 22, 10), 0xD69B1E));
        line(16, 10.5, 17.2, 4.5, 1.6, 0x6B4426);
        AffineTransform old = g.getTransform();
        g.rotate(Math.toRadians(-30), 21.5, 6);
        fill(new Ellipse2D.Double(18, 4.2, 7, 3.6), 0x6FBF3F);
        g.setTransform(old);
        shine(10, 14.5, 1.8, 2.6);
    }

    static void speed() {
        Path2D bolt = poly(19.5, 2.5, 7.5, 17.5, 14.8, 17.5, 11, 29.5, 25.5, 13, 18.2, 13, 23, 2.5);
        fill(bolt, 0x5CC8FF);
        clipped(bolt, () -> fill(poly(0, 0, 19.5, 0, 9.5, 17.5, 0, 17.5), 0xB9E9FF));
    }

    static void trident() {
        line(5.5, 27.5, 20.5, 12.5, 2.6, 0x2B8F80);
        double hx = 21.5, hy = 10.5;
        double dx = Math.sqrt(0.5), dy = -Math.sqrt(0.5); // up-right
        double px = Math.sqrt(0.5), py = Math.sqrt(0.5);  // perpendicular
        double ax = hx - px * 4, ay = hy - py * 4, bx = hx + px * 4, by = hy + py * 4;
        line(ax, ay, bx, by, 2.4, 0x5FD9C2);
        line(hx, hy, hx + dx * 9, hy + dy * 9, 2.2, 0x5FD9C2);
        line(ax, ay, ax + dx * 5.5, ay + dy * 5.5, 2.2, 0x5FD9C2);
        line(bx, by, bx + dx * 5.5, by + dy * 5.5, 2.2, 0x5FD9C2);
        line(5.5, 27.5, 8.5, 24.5, 3.0, 0x1F6A5F);
    }

    // ---------- post-processing ----------

    /** Adds a dark outline: dilate the silhouette by 1px (at 32x scale) and put it behind the art. */
    static BufferedImage outline(BufferedImage art) {
        int r = S; // 1 output pixel
        int[] a = new int[N * N];
        for (int y = 0; y < N; y++) for (int x = 0; x < N; x++) a[y * N + x] = art.getRGB(x, y) >>> 24;
        BufferedImage out = new BufferedImage(N, N, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int max = 0;
                for (int dy = -r; dy <= r && max < 255; dy++) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= N) continue;
                    for (int dx = -r; dx <= r; dx++) {
                        int xx = x + dx;
                        if (xx < 0 || xx >= N || dx * dx + dy * dy > r * r) continue;
                        max = Math.max(max, a[yy * N + xx]);
                    }
                }
                int oa = max * 235 / 255;
                int src = art.getRGB(x, y);
                int sa = src >>> 24;
                // src over outline (#17181D)
                double fa = sa / 255.0, ba = oa / 255.0 * (1 - fa);
                double outA = fa + ba;
                if (outA <= 0) { out.setRGB(x, y, 0); continue; }
                int rr = (int) Math.round((((src >> 16) & 255) * fa + 0x17 * ba) / outA);
                int gg = (int) Math.round((((src >> 8) & 255) * fa + 0x18 * ba) / outA);
                int bb = (int) Math.round(((src & 255) * fa + 0x1D * ba) / outA);
                out.setRGB(x, y, ((int) Math.round(outA * 255) << 24) | (rr << 16) | (gg << 8) | bb);
            }
        }
        return out;
    }

    /** Box filter SxS -> 1 with premultiplied alpha. */
    static BufferedImage downscale(BufferedImage src) {
        BufferedImage out = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                double sa = 0, sr = 0, sg = 0, sb = 0;
                for (int dy = 0; dy < S; dy++) for (int dx = 0; dx < S; dx++) {
                    int p = src.getRGB(x * S + dx, y * S + dy);
                    double al = (p >>> 24) / 255.0;
                    sa += al;
                    sr += ((p >> 16) & 255) * al;
                    sg += ((p >> 8) & 255) * al;
                    sb += (p & 255) * al;
                }
                if (sa == 0) continue;
                int A = (int) Math.round(sa / (S * S) * 255);
                out.setRGB(x, y, (A << 24) | ((int) Math.round(sr / sa) << 16) | ((int) Math.round(sg / sa) << 8) | (int) Math.round(sb / sa));
            }
        }
        return out;
    }
}
