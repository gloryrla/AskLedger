package askledger.service;

import askledger.model.Record;
import askledger.model.SearchQuery;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 없이 질문을 검색 조건으로 바꾸는 클래스.
 * 날짜 단어, 카테고리, 금액(이상/이하), 수입/지출 단어를 찾고 나머지 단어는 키워드로 쓴다.
 */
public class RuleParser {
    // 검색에 의미 없는 단어들 (키워드에서 뺀다)
    private static final List<String> STOP_WORDS = Arrays.asList(
            "how", "much", "did", "i", "on", "in", "the", "what", "show", "me", "won",
            "today", "this", "last", "week", "month", "months", "recent",
            "얼마", "얼마나", "내역", "보여줘", "원", "오늘", "이번", "지난", "주", "달",
            "이번주", "이번달", "지난달", "최근");
    // 지출/수입을 뜻하는 단어들
    private static final List<String> EXPENSE_WORDS = Arrays.asList("spend", "spent", "expense", "expenses", "paid", "지출", "썼어", "쓴");
    private static final List<String> INCOME_WORDS = Arrays.asList("income", "earned", "received", "수입", "받은");

    private List<String> categories;
    private Map<String, String> aliases = new HashMap<>();   // 카테고리를 다르게 부르는 말 (예: 커피 -> Cafe)

    public RuleParser(List<String> categories) {
        this.categories = categories;
        aliases.put("식비", "Food");
        aliases.put("카페", "Cafe");
        aliases.put("커피", "Cafe");
        aliases.put("교통", "Transport");
        aliases.put("택시", "Transport");
        aliases.put("쇼핑", "Shopping");
        aliases.put("월급", "Salary");
        aliases.put("용돈", "Allowance");
    }

    /** 오늘 날짜 기준 */
    public SearchQuery parse(String question) {
        return parse(question, LocalDate.now());
    }

    /** 질문 분석. 날짜 단어가 없으면 전체 기간을 검색 */
    public SearchQuery parse(String question, LocalDate today) {
        String text = question.toLowerCase().replace("?", " ").replace("!", " ").replace(".", " ").replace(",", "");
        String[] words = text.trim().split("\\s+");

        LocalDate start = null;
        LocalDate end = null;
        Record.Type type = null;
        Long min = null;
        Long max = null;
        List<String> cats = new ArrayList<>();
        List<String> keywords = new ArrayList<>();

        // 1. 날짜 단어 찾기
        if (text.contains("today") || text.contains("오늘")) {
            start = today;
            end = today;
        } else if (text.contains("this week") || text.contains("이번주") || text.contains("이번 주")) {
            int dayOfWeek = today.getDayOfWeek().getValue();
            start = today.minusDays(dayOfWeek - 1);
            end = start.plusDays(6);
        } else if (text.contains("last month") || text.contains("지난달") || text.contains("지난 달") || text.contains("저번달")) {
            LocalDate lastMonth = today.minusMonths(1);
            start = lastMonth.withDayOfMonth(1);
            end = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
        } else if (text.contains("this month") || text.contains("이번달") || text.contains("이번 달")) {
            start = today.withDayOfMonth(1);
            end = today.withDayOfMonth(today.lengthOfMonth());
        }

        // 2. 단어 하나씩 확인
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            String next = "";
            if (i + 1 < words.length) {
                next = words[i + 1];
            }

            // "recent 3 months", "최근 3개월"
            if (w.equals("recent") || w.equals("최근")) {
                String n = next.replace("개월", "");
                if (isNumber(n)) {
                    start = today.minusMonths(Integer.parseInt(n));
                    end = today;
                    i++;
                    continue;
                }
            }

            // "over 10000", "under 20000"
            if ((w.equals("over") || w.equals("above")) && toAmount(next) != null) {
                min = toAmount(next);
                i++;
                continue;
            }
            if ((w.equals("under") || w.equals("below")) && toAmount(next) != null) {
                max = toAmount(next);
                i++;
                continue;
            }

            // "5만원 이상", "3000원 이하"
            Long amount = toAmount(w);
            if (amount != null) {
                if (next.equals("이상")) {
                    min = amount;
                    i++;
                } else if (next.equals("이하")) {
                    max = amount;
                    i++;
                }
                continue;
            }

            // 수입/지출 단어
            if (EXPENSE_WORDS.contains(w)) {
                type = Record.Type.EXPENSE;
                continue;
            }
            if (INCOME_WORDS.contains(w)) {
                type = Record.Type.INCOME;
                continue;
            }

            // 카테고리 이름
            String category = findCategory(w);
            if (category != null) {
                if (!cats.contains(category)) {
                    cats.add(category);
                }
                continue;
            }

            // 나머지는 키워드
            if (!STOP_WORDS.contains(w)) {
                keywords.add(w);
            }
        }
        return new SearchQuery(start, end, type, cats, min, max, keywords);
    }

    // "10000" -> 10000, "5만원" -> 50000. 금액이 아니면 null
    private Long toAmount(String word) {
        String w = word.replace("won", "").replace("원", "");
        long unit = 1;
        if (w.endsWith("만")) {
            unit = 10000;
            w = w.substring(0, w.length() - 1);
        }
        if (!isNumber(w)) {
            return null;
        }
        return Long.parseLong(w) * unit;
    }

        private boolean isNumber(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // 카테고리 찾기
    private String findCategory(String word) {
        for (String c : categories) {
            if (c.equalsIgnoreCase(word)) {
                return c;
            }
        }
        // "식비를" 처럼 조사가 붙어도 찾도록 startsWith 사용
        for (String key : aliases.keySet()) {
            if (word.startsWith(key) && categories.contains(aliases.get(key))) {
                return aliases.get(key);
            }
        }
        return null;
    }
}
