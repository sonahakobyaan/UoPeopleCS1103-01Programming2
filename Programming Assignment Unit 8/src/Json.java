import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal recursive-descent JSON parser.
 *
 * <p>It exists so the project needs no external libraries (no Gson/Jackson jar
 * to download). JSON values are mapped to plain Java types:
 * object -> Map, array -> List, string -> String, number -> Double,
 * true/false -> Boolean, null -> null.</p>
 */
public final class Json {

    private final String text;   // the JSON document being parsed
    private int pos;             // index of the next character to read

    private Json(String text) {
        this.text = text;
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Parses a complete JSON document and returns the root value. */
    public static Object parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("JSON text is null");
        }
        Json parser = new Json(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("Unexpected trailing characters");
        }
        return value;
    }

    /** Casts a parsed value to a JSON object (Map). */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new IllegalArgumentException("Expected a JSON object");
    }

    /** Returns the nested object stored under {@code key}. */
    public static Map<String, Object> getObject(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return asObject(value);
    }

    /** Returns the array stored under {@code key} (empty list if absent). */
    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List) {
            return (List<Object>) value;
        }
        throw new IllegalArgumentException("Field is not an array: " + key);
    }

    /** Returns a string field, or {@code fallback} when missing. */
    public static String getString(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : value.toString();
    }

    /** Returns a numeric field, or {@code fallback} when missing. */
    public static double getNumber(Map<String, Object> map, String key, double fallback) {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    // ------------------------------------------------------------------
    // Parsing internals
    // ------------------------------------------------------------------

    private Object readValue() {
        if (pos >= text.length()) {
            throw error("Unexpected end of input");
        }
        char c = text.charAt(pos);
        switch (c) {
            case '{': return readObject();
            case '[': return readArray();
            case '"': return readString();
            case 't': expectWord("true");  return Boolean.TRUE;
            case 'f': expectWord("false"); return Boolean.FALSE;
            case 'n': expectWord("null");  return null;
            default:  return readNumber();
        }
    }

    private Map<String, Object> readObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        pos++;                                   // skip '{'
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("Expected a string key");
            }
            String key = readString();
            skipWhitespace();
            expectChar(':');
            skipWhitespace();
            result.put(key, readValue());
            skipWhitespace();
            if (peek() == ',') {
                pos++;
            } else {
                expectChar('}');
                return result;
            }
        }
    }

    private List<Object> readArray() {
        List<Object> result = new ArrayList<>();
        pos++;                                   // skip '['
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            result.add(readValue());
            skipWhitespace();
            if (peek() == ',') {
                pos++;
            } else {
                expectChar(']');
                return result;
            }
        }
    }

    private String readString() {
        StringBuilder sb = new StringBuilder();
        pos++;                                   // skip opening quote
        while (pos < text.length()) {
            char c = text.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= text.length()) {
                break;
            }
            char escaped = text.charAt(pos++);
            switch (escaped) {
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'r': sb.append('\r'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'u':
                    if (pos + 4 > text.length()) {
                        throw error("Bad unicode escape");
                    }
                    sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    pos += 4;
                    break;
                default:  sb.append(escaped);    // covers \" \\ and \/
            }
        }
        throw error("Unterminated string");
    }

    private Double readNumber() {
        int start = pos;
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        if (start == pos) {
            throw error("Unexpected character '" + text.charAt(pos) + "'");
        }
        try {
            return Double.parseDouble(text.substring(start, pos));
        } catch (NumberFormatException e) {
            throw error("Malformed number");
        }
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= text.length()) {
            throw error("Unexpected end of input");
        }
        return text.charAt(pos);
    }

    private void expectChar(char expected) {
        if (peek() != expected) {
            throw error("Expected '" + expected + "'");
        }
        pos++;
    }

    private void expectWord(String word) {
        if (!text.startsWith(word, pos)) {
            throw error("Expected " + word);
        }
        pos += word.length();
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + pos);
    }
}
