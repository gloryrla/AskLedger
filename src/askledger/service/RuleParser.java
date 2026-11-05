package askledger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a question into search conditions without the LLM.
 * Understands date words, category names, amount expressions and income/expense words
 * in English and Korean. The remaining words become keywords.
 */
public class RuleParser {
    private static final Pattern RECENT = Pattern.compile("(?:recent|last|past)\\s+(\\d+)\\s+months?|최근\\s*(\\d+)\\s*개월");
    private static final String NUM = "(\\d[\\d,]*)\\s*(만)?\\s*(?:won|원)?";
    private static final Pattern MIN = Pattern.compile("(?:over|more than|above|at least)\\s*" + NUM + "|" + NUM + "\\s*(?:or more|이상|넘게|넘는)");
    private static final Pattern MAX = Pattern.compile("(?:under|less than|below|at most)\\s*" + NUM + "|" + NUM + "\\s*(?:or less|이하|미만)");
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("식비", "Food"), Map.entry("meal", "Food"), Map.entry("밥", "Food"),
            Map.entry("카페", "Cafe"), Map.entry("coffee", "Cafe"), Map.entry("커피", "Cafe"),
            Map.entry("교통", "Transport"), Map.entry("bus", "Transport"), Map.entry("taxi", "Transport"),
            Map.entry("쇼핑", "Shopping"), Map.entry("문화", "Culture"), Map.entry("movie", "Culture"),
            Map.entry("교육", "Education"), Map.entry("rent", "Housing"), Map.entry("월세", "Housing"),
            Map.entry("병원", "Health"), Map.entry("구독", "Subscription"), Map.entry("월급", "Salary"),
            Map.entry("급여", "Salary"), Map.entry("용돈", "Allowance"), Map.entry("장학금", "Scholarship"));
    private static final Set<String> EXPENSE_WORDS = Set.of("spend", "spent", "expense", "expenses", "paid", "pay", "지출", "썼어", "썼지", "쓴", "썼나");
    private static final Set<String> INCOME_WORDS = Set.of("income", "earn", "earned", "received", "수입", "번", "받은");
    private static final Set<String> STOP = Set.of("how", "much", "many", "did", "do", "i", "on", "in", "the", "a", "an",
            "for", "my", "what", "was", "were", "show", "me", "find", "all", "records", "record", "of", "at", "to",
            "and", "is", "are", "total", "money", "with", "during", "won", "get", "got", "list",
            "얼마", "얼마나", "내역", "보여줘", "알려줘", "거", "것", "돈", "총", "나", "내가", "그", "했던", "했어", "원");
    private static final String[] JOSA = {"에서", "으로", "에", "로", "을", "를", "이", "가", "은", "는", "도", "의", "랑"};

    private final List<String> categories;

    /**
     * @param categories known category names
     */
    public RuleParser(List<String> categories) { this.categories = categories; }

    /**
     * Parses a question using today's date.
     * @param question the user's question
     * @return search conditions (never null)
     */
    public SearchQuery parse(String question) { return parse(question, LocalDate.now()); }

    /**
     * Parses a question relative to the given date. If no date word is found, all periods are searched.
     * @param question the user's question
     * @param today the date treated as today
     * @return search conditions (never null)
     */
    public SearchQuery parse(String question, LocalDate today) {
        String s = " " + question.toLowerCase().replaceAll("[?!.]", " ") + " ";
        LocalDate start = null, end = null;

        Matcher m = RECENT.matcher(s);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(2));
            start = today.minusMonths(n);
            end = today;
            s = m.replaceFirst(" ");
        } else {
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            Object[][] words = {
                    {"today|오늘", today, today},
                    {"yesterday|어제", today.minusDays(1), today.minusDays(1)},
                    {"this week|이번\\s*주", monday, monday.plusDays(6)},
                    {"last week|지난\\s*주|저번\\s*주", monday.minusWeeks(1), monday.minusDays(1)},
                    {"this month|이번\\s*달", today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth())},
                    {"last month|지난\\s*달|저번\\s*달", today.minusMonths(1).withDayOfMonth(1), today.withDayOfMonth(1).minusDays(1)},
                    {"this year|올해", today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear())},
                    {"last year|작년", today.minusYears(1).withDayOfYear(1), today.withDayOfYear(1).minusDays(1)},
            };
            for (Object[] w : words) {
                Matcher dm = Pattern.compile((String) w[0]).matcher(s);
                if (dm.find()) {
                    start = (LocalDate) w[1];
                    end = (LocalDate) w[2];
                    s = dm.replaceFirst(" ");
                    break;
                }
            }
        }

        Long min = null, max = null;
        Matcher mn = MIN.matcher(s);
        if (mn.find()) { min = amount(mn, 1); s = mn.replaceFirst(" "); }
        Matcher mx = MAX.matcher(s);
        if (mx.find()) { max = amount(mx, 1); s = mx.replaceFirst(" "); }

        Record.Type type = null;
        List<String> cats = new ArrayList<>();
        List<String> keywords = new ArrayList<>();
        for (String raw : s.replaceAll("[^\\p{L}\\p{N}/ ]", " ").trim().split("\\s+")) {
            if (raw.isEmpty()) continue;
            if (EXPENSE_WORDS.contains(raw)) { type = Record.Type.EXPENSE; continue; }
            if (INCOME_WORDS.contains(raw)) { type = Record.Type.INCOME; continue; }
            String w = stripJosa(raw);
            String cat = category(w);
            if (cat != null) { if (!cats.contains(cat)) cats.add(cat); continue; }
            if (STOP.contains(w) || STOP.contains(raw) || w.matches("\\d+") || w.matches("[a-z]")) continue;
            keywords.add(w);
        }
        return new SearchQuery(start, end, type, cats, min, max, keywords);
    }

    /** Reads the number of the first group pair that matched, applying the 만 (x10,000) unit. */
    private static Long amount(Matcher m, int first) {
        for (int g = first; g < m.groupCount(); g += 2) {
            if (m.group(g) != null) {
                long v = Long.parseLong(m.group(g).replace(",", ""));
                return m.group(g + 1) != null ? v * 10_000 : v;
            }
        }
        return null;
    }

    private String category(String w) {
        for (String c : categories) if (c.equalsIgnoreCase(w)) return c;
        String a = ALIASES.get(w);
        return a != null && categories.contains(a) ? a : null;
    }

    private static String stripJosa(String w) {
        for (String j : JOSA)
            if (w.length() > j.length() + 1 && w.endsWith(j) && !w.matches("[a-z0-9/]+")) return w.substring(0, w.length() - j.length());
        return w;
    }
}
