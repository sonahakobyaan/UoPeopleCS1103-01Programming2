import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.Random;
import javax.swing.JPanel;

/**
 * The window's content pane. It paints a vertical gradient whose colours
 * depend on the {@link TimeOfDay} of the searched city, plus stars at night
 * so the whole app "feels" like the local time.
 */
@SuppressWarnings("serial")
public class BackgroundPanel extends JPanel {

    private static final int STAR_COUNT = 70;

    private TimeOfDay phase = TimeOfDay.DAY;
    private final double[][] stars = new double[STAR_COUNT][3];   // x, y (0..1), radius

    public BackgroundPanel() {
        Random random = new Random(42);          // fixed seed: stars stay in place
        for (double[] star : stars) {
            star[0] = random.nextDouble();
            star[1] = random.nextDouble() * 0.7;
            star[2] = 0.8 + random.nextDouble() * 1.6;
        }
    }

    /** Changes the background and repaints. */
    public void setPhase(TimeOfDay phase) {
        this.phase = phase;
        repaint();
    }

    public TimeOfDay getPhase() {
        return phase;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        g2.setPaint(new GradientPaint(0, 0, phase.top(), 0, h, phase.bottom()));
        g2.fillRect(0, 0, w, h);

        if (phase.isDark()) {
            g2.setColor(new Color(255, 255, 255, 200));
            for (double[] star : stars) {
                double r = star[2];
                g2.fill(new Ellipse2D.Double(star[0] * w, star[1] * h, r * 2, r * 2));
            }
        }

        g2.dispose();
    }
}
