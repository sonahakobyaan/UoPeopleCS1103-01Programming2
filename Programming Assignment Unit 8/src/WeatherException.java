/**
 * Checked exception for every failure the weather service can report.
 * The {@link Kind} lets the GUI show a precise, user-friendly message.
 */
@SuppressWarnings("serial")
public class WeatherException extends Exception {

    /** The category of failure. */
    public enum Kind {
        EMPTY_INPUT,        // nothing typed in the location box
        INVALID_LOCATION,   // malformed coordinates / rejected by the API (400)
        NOT_FOUND,          // city does not exist (404)
        INVALID_API_KEY,    // missing or rejected key (401)
        RATE_LIMITED,       // too many requests (429)
        NETWORK,            // no internet, timeout, DNS failure
        SERVER,             // any other non-200 HTTP status
        BAD_RESPONSE        // 200 OK but the JSON could not be understood
    }

    private final Kind kind;

    public WeatherException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public WeatherException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
