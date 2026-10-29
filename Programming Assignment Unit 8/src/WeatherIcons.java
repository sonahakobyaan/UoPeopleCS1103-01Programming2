import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;

/**
 * Weather icons drawn with Java2D (no image files or downloads needed, so the
 * icons always work offline). Icons are chosen from the OpenWeatherMap icon
 * code, e.g. "01d" = clear day, "10n" = rain at night.
 */
public final class WeatherIcons {

    private static final Color SUN = new Color(255, 200, 40);
    private static final Color MOON = new Color(245, 240, 200);
    private static final Color CLOUD_LIGHT = new Color(240, 244, 250);
    private static final Color CLOUD_GREY = new Color(150, 160, 175);
    private static final Color CLOUD_DARK = new Color(95, 105, 125);
    private static final Color RAIN = new Color(90, 170, 255);

    private WeatherIcons() { }   // static utility class

    /** Reusable Swing component that shows one icon and can change it. */
    @SuppressWarnings("serial")
    public static final class IconComponent extends JComponent {
        private String code = "01d";

        public IconComponent(int size) {
            setPreferredSize(new Dimension(size, size));
            setOpaque(false);
        }

        public void setCode(String code) {
            this.code = code;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int size = Math.min(getWidth(), getHeight());      // keep the icon square and centred
            draw((Graphics2D) g, code, (getWidth() - size) / 2, (getHeight() - size) / 2, size);
        }
    }

    /** Draws the icon for {@code code} inside the square (x, y, size, size). */
    public static void draw(Graphics2D target, String code, int x, int y, int size) {
        Graphics2D g = (Graphics2D) target.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.translate(x, y);
        g.scale(size / 100.0, size / 100.0);          // draw on a 100 x 100 canvas

        boolean night = code.endsWith("n");
        String kind = code.length() >= 2 ? code.substring(0, 2) : "";

        switch (kind) {
            case "01" -> sunOrMoon(g, night, 50, 50, 1.0);
            case "02" -> {
                sunOrMoon(g, night, 36, 36, 0.7);
                cloud(g, 56, 62, 0.8, CLOUD_LIGHT);
            }
            case "03" -> cloud(g, 50, 55, 1.0, CLOUD_LIGHT);
            case "04" -> {
                cloud(g, 40, 42, 0.75, CLOUD_GREY);
                cloud(g, 58, 60, 0.9, CLOUD_LIGHT);
            }
            case "09" -> {
                cloud(g, 50, 38, 1.0, CLOUD_GREY);
                rain(g);
            }
            case "10" -> {
                sunOrMoon(g, night, 32, 28, 0.5);
                cloud(g, 54, 40, 0.95, CLOUD_GREY);
                rain(g);
            }
            case "11" -> {
                cloud(g, 50, 36, 1.0, CLOUD_DARK);
                bolt(g);
            }
            case "13" -> {
                cloud(g, 50, 38, 1.0, CLOUD_LIGHT);
                snow(g);
            }
            case "50" -> mist(g);
            default -> cloud(g, 50, 55, 1.0, CLOUD_LIGHT);
        }
        g.dispose();
    }

    // ------------------------------------------------------------------
    // Shape helpers (all coordinates are on the 100 x 100 canvas)
    // ------------------------------------------------------------------

    private static void sunOrMoon(Graphics2D g, boolean night, double cx, double cy, double s) {
        if (night) {                                   // crescent = disc minus offset disc
            Area moon = new Area(new Ellipse2D.Double(cx - 22 * s, cy - 22 * s, 44 * s, 44 * s));
            moon.subtract(new Area(new Ellipse2D.Double(cx - 22 * s + 14 * s, cy - 22 * s - 6 * s,
                    40 * s, 40 * s)));
            g.setColor(MOON);
            g.fill(moon);
            return;
        }
        g.setColor(SUN);
        g.fill(new Ellipse2D.Double(cx - 18 * s, cy - 18 * s, 36 * s, 36 * s));
        g.setStroke(new BasicStroke((float) (4 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 8; i++) {                  // eight rays around the disc
            double angle = Math.PI / 4 * i;
            g.draw(new Line2D.Double(
                    cx + Math.cos(angle) * 25 * s, cy + Math.sin(angle) * 25 * s,
                    cx + Math.cos(angle) * 35 * s, cy + Math.sin(angle) * 35 * s));
        }
    }

    private static void cloud(Graphics2D g, double cx, double cy, double s, Color color) {
        Area shape = new Area(new RoundRectangle2D.Double(cx - 32 * s, cy - 4 * s, 64 * s, 26 * s,
                26 * s, 26 * s));
        shape.add(new Area(new Ellipse2D.Double(cx - 22 * s, cy - 20 * s, 30 * s, 30 * s)));
        shape.add(new Area(new Ellipse2D.Double(cx - 4 * s, cy - 28 * s, 36 * s, 36 * s)));
        g.setColor(color);
        g.fill(shape);
    }

    private static void rain(Graphics2D g) {
        g.setColor(RAIN);
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int dx = -18; dx <= 18; dx += 18) {
            g.draw(new Line2D.Double(50 + dx, 68, 44 + dx, 84));
        }
    }

    private static void bolt(Graphics2D g) {
        Path2D bolt = new Path2D.Double();
        bolt.moveTo(54, 56);
        bolt.lineTo(40, 80);
        bolt.lineTo(50, 80);
        bolt.lineTo(44, 98);
        bolt.lineTo(66, 72);
        bolt.lineTo(55, 72);
        bolt.lineTo(62, 56);
        bolt.closePath();
        g.setColor(SUN);
        g.fill(bolt);
    }

    private static void snow(Graphics2D g) {
        g.setColor(Color.WHITE);
        double[][] flakes = {{34, 72}, {50, 84}, {66, 72}, {50, 66}};
        for (double[] f : flakes) {
            g.fill(new Ellipse2D.Double(f[0] - 5, f[1] - 5, 10, 10));
        }
    }

    private static void mist(Graphics2D g) {
        g.setColor(CLOUD_LIGHT);
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            int y = 30 + i * 15;
            int offset = (i % 2 == 0) ? 0 : 10;
            g.draw(new Line2D.Double(20 + offset, y, 76 + offset, y));
        }
    }
}
