import java.time.LocalDateTime;
import java.util.List;

/**
 * Offline self-test: checks parsing, unit conversion, input validation, error
 * mapping, history and time-of-day logic using sample JSON. No internet and no
 * API key needed. Run with:  java -cp out SelfTest
 */
public class SelfTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        testUnitConversion();
        testInputValidation();
        testHttpStatusMapping();
        testParsing();
        testTimeOfDay();
        testHistory();
        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---- individual tests ------------------------------------------------

    private static void testUnitConversion() {
        check("0 C -> 32 F", near(UnitConverter.celsiusToFahrenheit(0), 32.0));
        check("100 C -> 212 F", near(UnitConverter.celsiusToFahrenheit(100), 212.0));
        check("-40 C == -40 F", near(UnitConverter.celsiusToFahrenheit(-40), -40.0));
        check("10 m/s -> 36 km/h", near(UnitConverter.convertWind(10, UnitConverter.WindUnit.KILOMETRES_PER_HOUR), 36.0));
        check("10 m/s -> 22.4 mph", near(UnitConverter.convertWind(10, UnitConverter.WindUnit.MILES_PER_HOUR), 22.37));
        check("format temperature", UnitConverter.formatTemperature(21.6, UnitConverter.TempUnit.CELSIUS).equals("22°C"));
        check("format wind", UnitConverter.formatWind(5, UnitConverter.WindUnit.KILOMETRES_PER_HOUR).equals("18.0 km/h"));
    }

    private static void testInputValidation() {
        expectKind("empty input", WeatherException.Kind.EMPTY_INPUT, () -> WeatherService.buildLocationParams("   "));
        expectKind("lat out of range", WeatherException.Kind.INVALID_LOCATION, () -> WeatherService.buildLocationParams("95, 10"));
        expectKind("lon out of range", WeatherException.Kind.INVALID_LOCATION, () -> WeatherService.buildLocationParams("10, 200"));
        try {
            check("coordinates", WeatherService.buildLocationParams("51.5, -0.12").equals("lat=51.5&lon=-0.12"));
            check("city is URL-encoded", WeatherService.buildLocationParams("New York").equals("q=New+York"));
        } catch (WeatherException e) {
            check("valid input must not throw", false);
        }
    }

    private static void testHttpStatusMapping() {
        expectKind("404", WeatherException.Kind.NOT_FOUND, () -> WeatherService.checkStatus(404, ""));
        expectKind("401", WeatherException.Kind.INVALID_API_KEY, () -> WeatherService.checkStatus(401, ""));
        expectKind("429", WeatherException.Kind.RATE_LIMITED, () -> WeatherService.checkStatus(429, ""));
        expectKind("500", WeatherException.Kind.SERVER, () -> WeatherService.checkStatus(500, ""));
        expectKind("garbage JSON", WeatherException.Kind.BAD_RESPONSE, () -> WeatherService.parseCurrent("{not json"));
    }

    private static void testParsing() throws WeatherException {
        LocalDateTime now = LocalDateTime.of(2026, 10, 28, 12, 0);
        String json = SampleData.currentJson("London", "GB", 51.5072, -0.1276, 14.0, 72, 5.5,
                "Clouds", "scattered clouds", "03d", now, 0);
        WeatherData w = WeatherService.parseCurrent(json);
        check("city parsed", w.city().equals("London"));
        check("temperature parsed", near(w.temperatureC(), 14.0));
        check("humidity parsed", w.humidityPercent() == 72);
        check("wind parsed", near(w.windSpeedMs(), 5.5));
        check("local hour is 12", w.localTime().getHour() == 12);

        List<ForecastEntry> forecast = WeatherService.parseForecast(SampleData.forecastJson(now, 0, 14.0));
        check("8 forecast slots", forecast.size() == 8);
        check("first slot is 3 h later", forecast.get(0).localTime().getHour() == 15);
    }

    private static void testTimeOfDay() {
        long sunrise = 1_000_000;
        long sunset = sunrise + 10 * 3600;
        check("noon is DAY", TimeOfDay.classify(sunrise + 5 * 3600, sunrise, sunset, 12) == TimeOfDay.DAY);
        check("sunrise is DAWN", TimeOfDay.classify(sunrise, sunrise, sunset, 7) == TimeOfDay.DAWN);
        check("sunset is DUSK", TimeOfDay.classify(sunset, sunrise, sunset, 17) == TimeOfDay.DUSK);
        check("late is NIGHT", TimeOfDay.classify(sunset + 3 * 3600, sunrise, sunset, 21) == TimeOfDay.NIGHT);
        check("fallback uses hour", TimeOfDay.classify(0, 0, 0, 23) == TimeOfDay.NIGHT);
    }

    private static void testHistory() {
        SearchHistory h = new SearchHistory(2);
        LocalDateTime t = LocalDateTime.of(2026, 10, 28, 9, 0);
        h.add("a", "A", t);
        h.add("b", "B", t.plusMinutes(1));
        h.add("c", "C", t.plusMinutes(2));
        check("capacity respected", h.size() == 2);
        check("newest first", h.entries().get(0).query().equals("c"));
        check("timestamp shown", h.entries().get(0).display().startsWith("2026-10-28 09:02:00"));
    }

    // ---- tiny test framework ---------------------------------------------

    private interface Action {
        void run() throws Exception;
    }

    private static void expectKind(String name, WeatherException.Kind kind, Action action) {
        try {
            action.run();
            check(name + " should throw", false);
        } catch (WeatherException e) {
            check(name, e.getKind() == kind);
        } catch (Exception e) {
            check(name + " threw wrong type", false);
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 0.05;
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS  " : "FAIL  ") + name);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }
}
