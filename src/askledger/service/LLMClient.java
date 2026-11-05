package askledger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends the question to the LLM API (Anthropic Claude Messages API) and converts the answer
 * into search conditions. Only the question, today's date and the category names are sent;
 * records are never sent.
 * <p>The API key is read from the {@code ANTHROPIC_API_KEY} environment variable and the model
 * from {@code ASKLEDGER_MODEL} (default {@value #DEFAULT_MODEL}).</p>
 */
public class LLMClient {
    /** Model used when ASKLEDGER_MODEL is not set. */
    public static final String DEFAULT_MODEL = "claude-haiku-5-5";
    /** Maximum waiting time for an answer. */
    public static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final List<String> categories;
    private final String apiKey = System.getenv("ANTHROPIC_API_KEY");
    private final String model = System.getenv().getOrDefault("ASKLEDGER_MODEL", DEFAULT_MODEL);
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    /**
     * @param categories known category names, sent to the LLM
     */
    public LLMClient(List<String> categories) { this.categories = categories; }

    /**
     * Asks the LLM to turn the question into search conditions.
     * @param question the user's question
     * @return search conditions, or null if there is no answer within 10 seconds, no internet
     *         connection, no API key, or the answer cannot be read
     */
    @SuppressWarnings("unchecked")
    public SearchQuery requestQuery(String question) {
        if (apiKey == null || apiKey.isBlank()) return null;
        try {
            String system = "You convert a household account book question into search conditions. "
                    + "Today is " + LocalDate.now() + ". Known categories: " + String.join(", ", categories) + ". "
                    + "Answer with ONE JSON object only, no other text, with these keys: "
                    + "startDate, endDate (yyyy-MM-dd or null), type (\"INCOME\", \"EXPENSE\" or null), "
                    + "categories (array of known categories, may be empty), minAmount, maxAmount (integer won or null), "
                    + "keywords (array of words to find in the memo, may be empty). "
                    + "Do not guess amounts that the question does not mention.";
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("max_tokens", 300);
            body.put("system", system);
            body.put("messages", List.of(Map.of("role", "user", "content", question)));
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
                    .timeout(TIMEOUT)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(Json.write(body)))
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return null;
            Map<String, Object> root = (Map<String, Object>) Json.parse(res.body());
            String text = (String) ((Map<String, Object>) ((List<Object>) root.get("content")).get(0)).get("text");
            return toQuery(text);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts the JSON text answered by the LLM into a SearchQuery.
     * @param text answer that contains one JSON object
     * @return search conditions
     * @throws RuntimeException if the text cannot be read
     */
    @SuppressWarnings("unchecked")
    static SearchQuery toQuery(String text) {
        Map<String, Object> j = (Map<String, Object>) Json.parse(text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1));
        String type = (String) j.get("type");
        return new SearchQuery(date(j.get("startDate")), date(j.get("endDate")),
                type == null ? null : Record.Type.valueOf(type),
                strings(j.get("categories")), num(j.get("minAmount")), num(j.get("maxAmount")), strings(j.get("keywords")));
    }

    private static LocalDate date(Object o) { return o == null ? null : LocalDate.parse((String) o); }
    private static Long num(Object o) { return o == null ? null : ((Number) o).longValue(); }
    private static List<String> strings(Object o) {
        List<String> l = new ArrayList<>();
        if (o instanceof List<?> list) list.forEach(x -> l.add(String.valueOf(x)));
        return l;
    }
}
