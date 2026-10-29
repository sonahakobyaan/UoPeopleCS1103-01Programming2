import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * Hand-written JSON in the exact shape OpenWeatherMap returns. Used by the
 * offline self-test and by the screenshot tool, so neither needs internet
 * access or an API key.
 */
public final class SampleData {

    private SampleData() { }

    private static long epoch(LocalDateTime localTime, int tzSeconds) {
        return localTime.toEpochSecond(ZoneOffset.UTC) - tzSeconds;
    }

    /** A /weather response for the given local observation time. */
    public static String currentJson(String city, String country, double lat, double lon,
                                     double tempC, int humidity, double windMs,
                                     String main, String description, String icon,
                                     LocalDateTime localNow, int tzSeconds) {
        LocalDateTime day = localNow.toLocalDate().atStartOfDay();
        return String.format(Locale.US,
                "{\"coord\":{\"lon\":%.4f,\"lat\":%.4f},"
                + "\"weather\":[{\"id\":800,\"main\":\"%s\",\"description\":\"%s\",\"icon\":\"%s\"}],"
                + "\"main\":{\"temp\":%.2f,\"feels_like\":%.2f,\"pressure\":1014,\"humidity\":%d},"
                + "\"wind\":{\"speed\":%.2f},\"dt\":%d,"
                + "\"sys\":{\"country\":\"%s\",\"sunrise\":%d,\"sunset\":%d},"
                + "\"timezone\":%d,\"name\":\"%s\",\"cod\":200}",
                lon, lat, main, description, icon, tempC, tempC - 1.5, humidity, windMs,
                epoch(localNow, tzSeconds), country,
                epoch(day.plusHours(7).plusMinutes(20), tzSeconds),
                epoch(day.plusHours(17).plusMinutes(5), tzSeconds),
                tzSeconds, city);
    }

    /** A /forecast response with eight 3-hour slots starting after {@code localNow}. */
    public static String forecastJson(LocalDateTime localNow, int tzSeconds, double baseTempC) {
        String[][] weather = {
            {"Clouds", "scattered clouds", "03"}, {"Clouds", "broken clouds", "04"},
            {"Rain", "light rain", "10"},         {"Rain", "moderate rain", "10"},
            {"Clouds", "overcast clouds", "04"},  {"Clear", "clear sky", "01"},
            {"Clear", "clear sky", "01"},         {"Clouds", "few clouds", "02"}};
        StringBuilder sb = new StringBuilder("{\"cod\":\"200\",\"list\":[");
        for (int i = 0; i < weather.length; i++) {
            LocalDateTime slot = localNow.plusHours(3L * (i + 1));
            String dayNight = (slot.getHour() >= 7 && slot.getHour() < 17) ? "d" : "n";
            if (i > 0) {
                sb.append(',');
            }
            sb.append(String.format(Locale.US,
                    "{\"dt\":%d,\"main\":{\"temp\":%.2f},"
                    + "\"weather\":[{\"main\":\"%s\",\"description\":\"%s\",\"icon\":\"%s%s\"}],"
                    + "\"wind\":{\"speed\":%.1f},\"pop\":%.2f}",
                    epoch(slot, tzSeconds), baseTempC + Math.sin(i / 2.0) * 3 - i * 0.4,
                    weather[i][0], weather[i][1], weather[i][2], dayNight,
                    3.0 + i * 0.3, weather[i][0].equals("Rain") ? 0.7 : 0.1));
        }
        sb.append("],\"city\":{\"timezone\":").append(tzSeconds).append("}}");
        return sb.toString();
    }
}
