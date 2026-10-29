import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Immutable snapshot of the current weather for one location.
 *
 * <p>All values are stored in metric units (Celsius, metres/second) exactly as
 * requested from the API. Conversion to the user's chosen units happens only at
 * display time in {@link UnitConverter}, so switching units never needs a new
 * network request.</p>
 */
public record WeatherData(
        String city,
        String country,
        double latitude,
        double longitude,
        double temperatureC,
        double feelsLikeC,
        int humidityPercent,
        double windSpeedMs,
        int pressureHpa,
        String condition,        // short group, e.g. "Rain"
        String description,      // longer text, e.g. "light rain"
        String iconCode,         // OpenWeatherMap code, e.g. "10d"
        long observedAtEpoch,    // seconds since 1970 (UTC)
        long sunriseEpoch,
        long sunsetEpoch,
        int timezoneOffsetSeconds) {

    /** The observation time expressed in the searched city's own time zone. */
    public ZonedDateTime localTime() {
        return Instant.ofEpochSecond(observedAtEpoch)
                .atZone(ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds));
    }

    /** "London, GB" style label used in the UI and the history list. */
    public String displayName() {
        return country.isEmpty() ? city : city + ", " + country;
    }
}
