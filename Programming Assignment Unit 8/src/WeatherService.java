import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Talks to the OpenWeatherMap REST API and converts its JSON answers into
 * {@link WeatherData} and {@link ForecastEntry} objects.
 *
 * <p>Two endpoints are used (both free-tier):</p>
 * <ul>
 *   <li>{@code /data/2.5/weather}  - current conditions</li>
 *   <li>{@code /data/2.5/forecast} - 3-hourly short-term forecast</li>
 * </ul>
 * Data is always requested in metric units; the GUI converts when displaying.
 */
public class WeatherService {

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String KEY_ENV_VAR = "OPENWEATHER_API_KEY";
    private static final String CONFIG_NAME = "config.properties";
    private static final String PLACEHOLDER = "PASTE_YOUR";
    private static final int FORECAST_SLOTS = 8;      // 8 x 3 h = next 24 hours

    /** Matches "lat, lon" input such as "51.5, -0.12". */
    private static final Pattern COORDINATES = Pattern.compile(
            "^\\s*(-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\s*$");

    /** Result of one search: the current weather plus its forecast. */
    public record Result(WeatherData current, List<ForecastEntry> forecast) { }

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private String apiKey;

    public WeatherService(String apiKey) {
        this.apiKey = apiKey;
    }

    // ------------------------------------------------------------------
    // API key handling (the key is never hard-coded in the source)
    // ------------------------------------------------------------------

    /**
     * Every place a config file is looked for, best first: the working directory,
     * the project folder next to the compiled classes, and the user's home folder.
     * A ".txt" twin is also checked because some editors silently add that suffix.
     */
    static List<Path> configCandidates() {
        List<Path> folders = new ArrayList<>();
        folders.add(Path.of("").toAbsolutePath());
        try {
            Path classes = Path.of(WeatherService.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            Path folder = Files.isDirectory(classes) ? classes : classes.getParent();
            folders.add(folder);
            if (folder.getParent() != null) {
                folders.add(folder.getParent());              // e.g. project root above out/ or bin/
            }
        } catch (Exception ignored) {
            // Code source unknown: the other folders are still searched.
        }
        folders.add(Path.of(System.getProperty("user.home", ".")));

        List<Path> candidates = new ArrayList<>();
        for (Path folder : folders) {
            candidates.add(folder.resolve(CONFIG_NAME));
            candidates.add(folder.resolve(CONFIG_NAME + ".txt"));
        }
        return candidates;
    }

    /** Reads the key from the environment, or from the first usable config file. */
    public static String loadApiKey() {
        String fromEnv = System.getenv(KEY_ENV_VAR);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        for (Path file : configCandidates()) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                continue;                                     // unreadable: try the next place
            }
            String key = props.getProperty("api.key", "").trim();
            if (!key.isEmpty() && !key.startsWith(PLACEHOLDER)) {
                return key;
            }
        }
        return null;
    }

    /** Stores a key typed into the GUI; returns the file it was saved to. */
    public static Path saveApiKey(String key) throws IOException {
        Properties props = new Properties();
        props.setProperty("api.key", key.trim());
        Path target = configCandidates().get(0);              // working directory first
        try (OutputStream out = Files.newOutputStream(target)) {
            props.store(out, "OpenWeatherMap API key");
        } catch (IOException e) {
            target = Path.of(System.getProperty("user.home", "."), CONFIG_NAME);
            try (OutputStream out = Files.newOutputStream(target)) {
                props.store(out, "OpenWeatherMap API key");
            }
        }
        return target;
    }

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    // ------------------------------------------------------------------
    // Public entry point
    // ------------------------------------------------------------------

    /**
     * Looks up a city name ("Paris", "London,GB") or coordinates ("48.85, 2.35").
     * Blocking - call it from a background thread, never the Swing event thread.
     */
    public Result fetch(String input) throws WeatherException {
        String locationParams = buildLocationParams(input);
        String currentJson = get("weather", locationParams);
        String forecastJson = get("forecast", locationParams + "&cnt=" + FORECAST_SLOTS);
        return new Result(parseCurrent(currentJson), parseForecast(forecastJson));
    }

    // ------------------------------------------------------------------
    // Input validation and request building
    // ------------------------------------------------------------------

    /**
     * Turns the user's text into the query-string part of the URL.
     * Package-private so the self-test can check it without network access.
     */
    static String buildLocationParams(String input) throws WeatherException {
        if (input == null || input.isBlank()) {
            throw new WeatherException(WeatherException.Kind.EMPTY_INPUT,
                    "Please type a city name or coordinates first.");
        }
        Matcher m = COORDINATES.matcher(input);
        if (m.matches()) {
            double lat = Double.parseDouble(m.group(1));
            double lon = Double.parseDouble(m.group(2));
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                throw new WeatherException(WeatherException.Kind.INVALID_LOCATION,
                        "Coordinates out of range (latitude -90..90, longitude -180..180).");
            }
            return "lat=" + lat + "&lon=" + lon;
        }
        return "q=" + URLEncoder.encode(input.trim(), StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // HTTP
    // ------------------------------------------------------------------

    private String get(String endpoint, String locationParams) throws WeatherException {
        if (!hasKey()) {
            throw new WeatherException(WeatherException.Kind.INVALID_API_KEY,
                    "No API key set. Add your OpenWeatherMap key first.");
        }
        URI uri = URI.create(BASE_URL + endpoint + "?" + locationParams
                + "&units=metric&appid=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        try {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            return checkStatus(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new WeatherException(WeatherException.Kind.NETWORK,
                    "Could not reach the weather service. Check your internet connection.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WeatherException(WeatherException.Kind.NETWORK,
                    "The request was interrupted.", e);
        }
    }

    /** Maps an HTTP status code to a body (200) or a descriptive exception. */
    static String checkStatus(int status, String body) throws WeatherException {
        switch (status) {
            case 200:
                return body;
            case 400:
                throw new WeatherException(WeatherException.Kind.INVALID_LOCATION,
                        "That location is not valid. Try a city name or 'lat, lon'.");
            case 401:
                throw new WeatherException(WeatherException.Kind.INVALID_API_KEY,
                        "The API key was rejected. New keys can take up to two hours to activate.");
            case 404:
                throw new WeatherException(WeatherException.Kind.NOT_FOUND,
                        "Location not found. Check the spelling and try again.");
            case 429:
                throw new WeatherException(WeatherException.Kind.RATE_LIMITED,
                        "Too many requests. Please wait a minute and try again.");
            default:
                throw new WeatherException(WeatherException.Kind.SERVER,
                        "The weather service returned an error (HTTP " + status + ").");
        }
    }

    // ------------------------------------------------------------------
    // JSON -> objects (static + package-private so they can be unit tested)
    // ------------------------------------------------------------------

    /** Parses the /weather response. */
    static WeatherData parseCurrent(String json) throws WeatherException {
        try {
            Map<String, Object> root = Json.asObject(Json.parse(json));
            Map<String, Object> coord = Json.getObject(root, "coord");
            Map<String, Object> main = Json.getObject(root, "main");
            Map<String, Object> sys = root.get("sys") == null
                    ? Map.of() : Json.getObject(root, "sys");
            Map<String, Object> wind = root.get("wind") == null
                    ? Map.of() : Json.getObject(root, "wind");
            Map<String, Object> weather = Json.asObject(Json.getList(root, "weather").get(0));

            return new WeatherData(
                    Json.getString(root, "name", "Unknown"),
                    Json.getString(sys, "country", ""),
                    Json.getNumber(coord, "lat", 0),
                    Json.getNumber(coord, "lon", 0),
                    Json.getNumber(main, "temp", 0),
                    Json.getNumber(main, "feels_like", 0),
                    (int) Json.getNumber(main, "humidity", 0),
                    Json.getNumber(wind, "speed", 0),
                    (int) Json.getNumber(main, "pressure", 0),
                    Json.getString(weather, "main", ""),
                    Json.getString(weather, "description", ""),
                    Json.getString(weather, "icon", "03d"),
                    (long) Json.getNumber(root, "dt", 0),
                    (long) Json.getNumber(sys, "sunrise", 0),
                    (long) Json.getNumber(sys, "sunset", 0),
                    (int) Json.getNumber(root, "timezone", 0));
        } catch (RuntimeException e) {
            throw new WeatherException(WeatherException.Kind.BAD_RESPONSE,
                    "The weather data could not be understood.", e);
        }
    }

    /** Parses the /forecast response into a list of 3-hour slots. */
    static List<ForecastEntry> parseForecast(String json) throws WeatherException {
        try {
            Map<String, Object> root = Json.asObject(Json.parse(json));
            int tz = (int) Json.getNumber(Json.getObject(root, "city"), "timezone", 0);
            List<ForecastEntry> result = new ArrayList<>();
            for (Object item : Json.getList(root, "list")) {
                Map<String, Object> slot = Json.asObject(item);
                Map<String, Object> weather = Json.asObject(Json.getList(slot, "weather").get(0));
                Map<String, Object> wind = slot.get("wind") == null
                        ? Map.of() : Json.getObject(slot, "wind");
                result.add(new ForecastEntry(
                        (long) Json.getNumber(slot, "dt", 0),
                        tz,
                        Json.getNumber(Json.getObject(slot, "main"), "temp", 0),
                        Json.getString(weather, "main", ""),
                        Json.getString(weather, "description", ""),
                        Json.getString(weather, "icon", "03d"),
                        Json.getNumber(wind, "speed", 0),
                        Json.getNumber(slot, "pop", 0)));
            }
            return result;
        } catch (RuntimeException e) {
            throw new WeatherException(WeatherException.Kind.BAD_RESPONSE,
                    "The forecast data could not be understood.", e);
        }
    }
}
