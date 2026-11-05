package askledger;

import java.util.List;

/**
 * Result of a search.
 * @param query the conditions that were used
 * @param records matching records
 * @param count number of records
 * @param total sum of the amounts, calculated by the program
 * @param usedRuleParser true if the LLM failed and RuleParser was used
 */
public record SearchResult(SearchQuery query, List<Record> records, int count, long total, boolean usedRuleParser) {
}
