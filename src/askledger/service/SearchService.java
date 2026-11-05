package askledger;

import java.util.List;

/**
 * Controls the search by question: LLM first, RuleParser if the LLM fails.
 * Counting and adding up are always done here, never by the LLM.
 */
public class SearchService {
    private final LLMClient llm;
    private final RuleParser ruleParser;
    private final RecordManager recordManager;
    private final List<String> categories;

    /**
     * @param llm LLM client
     * @param ruleParser fallback parser
     * @param recordManager source of records
     * @param categories known category names
     */
    public SearchService(LLMClient llm, RuleParser ruleParser, RecordManager recordManager, List<String> categories) {
        this.llm = llm;
        this.ruleParser = ruleParser;
        this.recordManager = recordManager;
        this.categories = categories;
    }

    /**
     * Searches records by a question. If the LLM gives no usable conditions, RuleParser is used.
     * @param question the user's question
     * @return matching records with count and total
     */
    public SearchResult search(String question) {
        SearchQuery q = llm.requestQuery(question);
        boolean rule = q == null || !validate(q);
        if (rule) q = ruleParser.parse(question);
        return summarize(q, recordManager.findRecords(q), rule);
    }

    /**
     * Searches again with conditions corrected by the user, without calling the LLM.
     * @param query corrected conditions
     * @return matching records with count and total
     * @throws IllegalArgumentException if the query is not valid
     */
    public SearchResult apply(SearchQuery query) {
        if (!validate(query))
            throw new IllegalArgumentException("Start date must not be after end date, amounts must not be negative, and categories must be known.");
        return summarize(query, recordManager.findRecords(query), false);
    }

    /**
     * Checks that the start date is not after the end date, amounts are not negative,
     * and the categories are known.
     * @param q conditions to check
     * @return false if the query cannot be used
     */
    public boolean validate(SearchQuery q) {
        if (q.startDate() != null && q.endDate() != null && q.startDate().isAfter(q.endDate())) return false;
        if (q.minAmount() != null && q.minAmount() < 0) return false;
        if (q.maxAmount() != null && q.maxAmount() < 0) return false;
        return q.categories() == null || categories.containsAll(q.categories());
    }

    /**
     * Counts the records and adds up their amounts.
     * @param q conditions that were used
     * @param records matching records
     * @param usedRuleParser true if RuleParser was used
     * @return the result
     */
    public SearchResult summarize(SearchQuery q, List<Record> records, boolean usedRuleParser) {
        return new SearchResult(q, records, records.size(), records.stream().mapToLong(Record::getAmount).sum(), usedRuleParser);
    }
}
