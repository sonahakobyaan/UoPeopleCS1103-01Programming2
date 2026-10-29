import java.awt.Color;

/**
 * Phase of the day at the searched location. Each phase carries the two
 * colours of the vertical gradient used for the dynamic window background.
 */
public enum TimeOfDay {

    NIGHT(new Color(8, 14, 44),   new Color(38, 50, 100)),
    DAWN (new Color(72, 84, 146), new Color(255, 172, 120)),
    DAY  (new Color(46, 138, 232), new Color(146, 208, 250)),
    DUSK (new Color(58, 48, 118), new Color(250, 118, 80));

    /** Length of the twilight window around sunrise and sunset (45 min). */
    private static final long TWILIGHT_SECONDS = 45 * 60;

    private final Color top;
    private final Color bottom;

    TimeOfDay(Color top, Color bottom) {
        this.top = top;
        this.bottom = bottom;
    }

    public Color top() {
        return top;
    }

    public Color bottom() {
        return bottom;
    }

    /** True for the phases where stars are drawn and the moon is shown. */
    public boolean isDark() {
        return this == NIGHT;
    }

    /**
     * Classifies the moment {@code nowEpoch} using the day's sunrise and
     * sunset (all in epoch seconds, as supplied by the API).
     * If sunrise/sunset are unknown (0) it falls back to the clock hour.
     */
    public static TimeOfDay classify(long nowEpoch, long sunriseEpoch, long sunsetEpoch,
                                     int localHour) {
        if (sunriseEpoch <= 0 || sunsetEpoch <= 0) {
            return fromHour(localHour);
        }
        if (nowEpoch >= sunriseEpoch - TWILIGHT_SECONDS
                && nowEpoch < sunriseEpoch + TWILIGHT_SECONDS) {
            return DAWN;
        }
        if (nowEpoch >= sunriseEpoch + TWILIGHT_SECONDS
                && nowEpoch < sunsetEpoch - TWILIGHT_SECONDS) {
            return DAY;
        }
        if (nowEpoch >= sunsetEpoch - TWILIGHT_SECONDS
                && nowEpoch < sunsetEpoch + TWILIGHT_SECONDS) {
            return DUSK;
        }
        return NIGHT;
    }

    /** Simple clock-based fallback: 5-7 dawn, 7-17 day, 17-19 dusk, else night. */
    public static TimeOfDay fromHour(int hour) {
        if (hour >= 5 && hour < 7) {
            return DAWN;
        }
        if (hour >= 7 && hour < 17) {
            return DAY;
        }
        if (hour >= 17 && hour < 19) {
            return DUSK;
        }
        return NIGHT;
    }

    /** Convenience overload that works straight from a {@link WeatherData}. */
    public static TimeOfDay of(WeatherData w) {
        return classify(w.observedAtEpoch(), w.sunriseEpoch(), w.sunsetEpoch(),
                w.localTime().getHour());
    }
}
