import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import javax.imageio.ImageIO;
import javax.swing.JComponent;

/**
 * Optional helper: renders the real GUI (using sample JSON, no network) to PNG
 * files. Handy for the assignment's screenshot requirement when you want a
 * repeatable image. Run with:  java -Djava.awt.headless=true -cp out ScreenshotTool
 */
public class ScreenshotTool {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 660;

    public static void main(String[] args) throws Exception {
        File dir = new File(args.length > 0 ? args[0] : "docs");
        dir.mkdirs();

        // 1. Daytime in London, Celsius + km/h.
        render(dir, "screenshot_day.png", "London", "GB", 51.5072, -0.1276, 14.3, 72, 5.5,
                "Clouds", "scattered clouds", "03d", LocalDateTime.of(2026, 10, 28, 12, 10), 0,
                UnitConverter.TempUnit.CELSIUS, UnitConverter.WindUnit.KILOMETRES_PER_HOUR, null);

        // 2. Night in Tokyo, Fahrenheit + mph.
        render(dir, "screenshot_night.png", "Tokyo", "JP", 35.6895, 139.6917, 17.8, 64, 3.1,
                "Clear", "clear sky", "01n", LocalDateTime.of(2026, 10, 28, 22, 40), 32400,
                UnitConverter.TempUnit.FAHRENHEIT, UnitConverter.WindUnit.MILES_PER_HOUR, null);

        // 3. Error state: unknown city.
        render(dir, "screenshot_error.png", "Paris", "FR", 48.8566, 2.3522, 11.2, 81, 4.2,
                "Rain", "light rain", "10d", LocalDateTime.of(2026, 10, 28, 9, 30), 3600,
                UnitConverter.TempUnit.CELSIUS, UnitConverter.WindUnit.METRES_PER_SECOND,
                "Location not found. Check the spelling and try again.");
        System.out.println("Screenshots written to " + dir.getAbsolutePath());
    }

    private static void render(File dir, String fileName, String city, String country,
                               double lat, double lon, double tempC, int humidity, double windMs,
                               String main, String description, String icon,
                               LocalDateTime localNow, int tz,
                               UnitConverter.TempUnit tempUnit, UnitConverter.WindUnit windUnit,
                               String errorMessage) throws Exception {
        WeatherApp app = new WeatherApp(new WeatherService("demo"));

        // A few earlier searches so the history panel has content.
        LocalDateTime stamp = LocalDateTime.of(2026, 10, 28, 11, 52, 14);
        app.getHistory().add("Madrid", "Madrid, ES", stamp);
        app.getHistory().add("40.71, -74.00", "New York, US", stamp.plusMinutes(3));
        app.getHistory().add("Sydney", "Sydney, AU", stamp.plusMinutes(5));

        String currentJson = SampleData.currentJson(city, country, lat, lon, tempC, humidity,
                windMs, main, description, icon, localNow, tz);
        String forecastJson = SampleData.forecastJson(localNow, tz, tempC);
        WeatherService.Result result = new WeatherService.Result(
                WeatherService.parseCurrent(currentJson), WeatherService.parseForecast(forecastJson));

        app.setUnits(tempUnit, windUnit);
        app.showResult(result, city);
        app.setLocationText(city);
        if (errorMessage != null) {
            app.showError(errorMessage);
        }

        JComponent root = app.getRoot();
        root.setSize(new Dimension(WIDTH, HEIGHT));
        layoutRecursively(root);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        root.paint(g);
        g.dispose();
        ImageIO.write(image, "png", new File(dir, fileName));
    }

    /** Headless components are never shown, so lay them out by hand. */
    private static void layoutRecursively(Component c) {
        if (c instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }
}
