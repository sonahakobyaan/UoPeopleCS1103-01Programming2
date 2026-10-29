import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * One 3-hour slot of the short-term forecast (metric units, like
 * {@link WeatherData}).
 */
public record ForecastEntry(
        long epoch,                  // forecast time, seconds since 1970 (UTC)
        int timezoneOffsetSeconds,   // city's offset from UTC
        double temperatureC,
        String condition,
        String description,
        String iconCode,
        double windSpeedMs,
        double rainChance) {         // probability of precipitation, 0.0 - 1.0

    /** The slot's time in the city's own time zone. */
    public ZonedDateTime localTime() {
        return Instant.ofEpochSecond(epoch)
                .atZone(ZoneOffset.ofTotalSeconds(timezoneOffsetSeconds));
    }
}
