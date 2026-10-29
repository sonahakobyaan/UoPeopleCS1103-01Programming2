import java.util.Locale;

/**
 * Pure unit-conversion logic (no GUI, no network), so it is easy to test.
 * Internally the program always stores Celsius and metres/second.
 */
public final class UnitConverter {

    private UnitConverter() { }   // static utility class

    /** Temperature units offered in the GUI. */
    public enum TempUnit {
        CELSIUS("°C"), FAHRENHEIT("°F");

        private final String symbol;

        TempUnit(String symbol) {
            this.symbol = symbol;
        }

        public String symbol() {
            return symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    /** Wind-speed units offered in the GUI, with their factor from m/s. */
    public enum WindUnit {
        METRES_PER_SECOND("m/s", 1.0),
        KILOMETRES_PER_HOUR("km/h", 3.6),
        MILES_PER_HOUR("mph", 2.236936),
        KNOTS("knots", 1.943844);

        private final String label;
        private final double perMetreSecond;

        WindUnit(String label, double perMetreSecond) {
            this.label = label;
            this.perMetreSecond = perMetreSecond;
        }

        public String label() {
            return label;
        }

        public double fromMetresPerSecond(double ms) {
            return ms * perMetreSecond;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** Celsius to Fahrenheit: F = C x 9/5 + 32. */
    public static double celsiusToFahrenheit(double celsius) {
        return celsius * 9.0 / 5.0 + 32.0;
    }

    /** Converts a Celsius value to the requested unit. */
    public static double convertTemperature(double celsius, TempUnit unit) {
        return unit == TempUnit.FAHRENHEIT ? celsiusToFahrenheit(celsius) : celsius;
    }

    /** Converts a speed in m/s to the requested unit. */
    public static double convertWind(double metresPerSecond, WindUnit unit) {
        return unit.fromMetresPerSecond(metresPerSecond);
    }

    /** e.g. "21°C" (rounded to a whole number). */
    public static String formatTemperature(double celsius, TempUnit unit) {
        return Math.round(convertTemperature(celsius, unit)) + unit.symbol();
    }

    /** e.g. "12.6 km/h" (one decimal place, locale-independent). */
    public static String formatWind(double metresPerSecond, WindUnit unit) {
        return String.format(Locale.US, "%.1f %s",
                convertWind(metresPerSecond, unit), unit.label());
    }
}
