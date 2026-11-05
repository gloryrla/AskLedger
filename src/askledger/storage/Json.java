package askledger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON reader and writer used for the data file and the LLM answer.
 * Objects become {@code Map<String,Object>}, arrays become {@code List<Object>},
 * numbers become {@code Long} or {@code Double}.
 */
public final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; }

    /**
     * Parses a JSON text.
     * @param text JSON text
     * @return Map, List, String, Long, Double, Boolean or null
     * @throws IllegalArgumentException if the text is not valid JSON
     */
    public static Object parse(String text) {
        Json p = new Json(text);
        Object v = p.value();
        p.ws();
        if (p.i != text.length()) throw p.err();
        return v;
    }

    /**
     * Converts a value (Map, List, String, Number, Boolean or null) to JSON text.
     * @param v value to write
     * @return JSON text
     */
    public static String write(Object v) {
        StringBuilder b = new StringBuilder();
        write(v, b);
        return b.toString();
    }

    private static void write(Object v, StringBuilder b) {
        if (v == null || v instanceof Number || v instanceof Boolean) { b.append(v); }
        else if (v instanceof Map<?, ?> m) {
            b.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) b.append(',');
                first = false;
                write(String.valueOf(e.getKey()), b);
                b.append(':');
                write(e.getValue(), b);
            }
            b.append('}');
        } else if (v instanceof List<?> l) {
            b.append('[');
            for (int k = 0; k < l.size(); k++) { if (k > 0) b.append(','); write(l.get(k), b); }
            b.append(']');
        } else {
            b.append('"');
            for (char c : v.toString().toCharArray()) {
                switch (c) {
                    case '"' -> b.append("\\\"");
                    case '\\' -> b.append("\\\\");
                    case '\n' -> b.append("\\n");
                    case '\r' -> b.append("\\r");
                    case '\t' -> b.append("\\t");
                    default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
                }
            }
            b.append('"');
        }
    }

    private Object value() {
        ws();
        if (i >= s.length()) throw err();
        char c = s.charAt(i);
        if (c == '{') {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; ws();
            if (peek('}')) return m;
            do { ws(); String k = string(); ws(); expect(':'); m.put(k, value()); ws(); } while (peek(','));
            expect('}');
            return m;
        }
        if (c == '[') {
            List<Object> l = new ArrayList<>();
            i++; ws();
            if (peek(']')) return l;
            do { l.add(value()); ws(); } while (peek(','));
            expect(']');
            return l;
        }
        if (c == '"') return string();
        if (s.startsWith("true", i)) { i += 4; return true; }
        if (s.startsWith("false", i)) { i += 5; return false; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        int st = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        String n = s.substring(st, i);
        if (n.isEmpty()) throw err();
        return n.matches("-?\\d+") ? (Object) Long.parseLong(n) : (Object) Double.parseDouble(n);
    }

    private String string() {
        expect('"');
        StringBuilder b = new StringBuilder();
        while (true) {
            if (i >= s.length()) throw err();
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c != '\\') { b.append(c); continue; }
            char e = s.charAt(i++);
            switch (e) {
                case 'n' -> b.append('\n');
                case 't' -> b.append('\t');
                case 'r' -> b.append('\r');
                case 'b' -> b.append('\b');
                case 'f' -> b.append('\f');
                case 'u' -> { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; }
                default -> b.append(e);
            }
        }
    }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
    private boolean peek(char c) { if (i < s.length() && s.charAt(i) == c) { i++; return true; } return false; }
    private void expect(char c) { if (!peek(c)) throw err(); }
    private IllegalArgumentException err() { return new IllegalArgumentException("Invalid JSON at " + i); }
}
