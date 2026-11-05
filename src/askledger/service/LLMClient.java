package askledger.service;

import askledger.model.Record;
import askledger.model.SearchQuery;
import askledger.storage.Json;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/** Claude API를 써서 질문을 검색 조건으로 바꾸는 클래스 (API 키는 환경변수 ANTHROPIC_API_KEY) */
public class LLMClient {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-5-5";
    private static final int TIMEOUT = 10000;   // 10초

    private List<String> categories;
    private String apiKey;

    public LLMClient(List<String> categories) {
        this.categories = categories;
        this.apiKey = System.getenv("ANTHROPIC_API_KEY");
    }

    /** LLM한테 질문을 보내서 검색 조건을 받아옴. 실패하면 null 리턴 */
    public SearchQuery requestQuery(String question) {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }
        try {
            // LLM에게 줄 안내문 (JSON 형식으로만 답하게 함)
            String prompt = "가계부 질문을 검색 조건으로 바꿔줘. "
                    + "오늘 날짜: " + LocalDate.now() + ", 카테고리 목록: " + String.join(", ", categories) + ". "
                    + "JSON 객체 하나로만 답해. 키는 다음과 같아: "
                    + "startDate, endDate (yyyy-MM-dd 또는 null), type (\"INCOME\", \"EXPENSE\" 또는 null), "
                    + "categories (카테고리 목록에 있는 것만 배열로), minAmount, maxAmount (정수 또는 null), "
                    + "keywords (메모에서 찾을 단어 배열).";
            String body = "{\"model\": " + Json.quote(MODEL)
                    + ", \"max_tokens\": 300"
                    + ", \"system\": " + Json.quote(prompt)
                    + ", \"messages\": [{\"role\": \"user\", \"content\": " + Json.quote(question) + "}]}";

            HttpURLConnection conn = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setDoOutput(true);
            conn.setRequestProperty("x-api-key", apiKey);
            conn.setRequestProperty("anthropic-version", "2023-06-01");
            conn.setRequestProperty("content-type", "application/json");

            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes(StandardCharsets.UTF_8));
            out.close();

            if (conn.getResponseCode() != 200) {
                return null;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String response = "";
            String line;
            while ((line = reader.readLine()) != null) {
                response += line;
            }
            reader.close();
            String answer = Json.getString(response, "text");
            if (answer == null) {
                return null;
            }
            return toQuery(answer);
        } catch (Exception e) {
            return null;
        }
    }

    /** LLM이 쓴 JSON 글을 SearchQuery로 바꾼다 */
    public static SearchQuery toQuery(String text) {
        Record.Type type = null;
        String typeText = Json.getString(text, "type");
        if ("INCOME".equals(typeText)) {
            type = Record.Type.INCOME;
        } else if ("EXPENSE".equals(typeText)) {
            type = Record.Type.EXPENSE;
        }
        return new SearchQuery(
                toDate(Json.getString(text, "startDate")),
                toDate(Json.getString(text, "endDate")),
                type,
                Json.getList(text, "categories"),
                Json.getLong(text, "minAmount"),
                Json.getLong(text, "maxAmount"),
                Json.getList(text, "keywords"));
    }

    // "2026-10-01" 같은 글자를 날짜로 바꾼다
    private static LocalDate toDate(String text) {
        if (text == null) {
            return null;
        }
        return LocalDate.parse(text);
    }
}
