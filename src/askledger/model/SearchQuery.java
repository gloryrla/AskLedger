package askledger;

import java.time.LocalDate;
import java.util.List;

/**
 * Search conditions. A {@code null} (or empty list) condition is ignored.
 * @param startDate first day (inclusive)
 * @param endDate last day (inclusive)
 * @param type income or expense
 * @param categories allowed categories
 * @param minAmount minimum amount
 * @param maxAmount maximum amount
 * @param keywords words looked up in the memo and category (any one must match)
 */
public record SearchQuery(LocalDate startDate, LocalDate endDate, Record.Type type, List<String> categories,
                          Long minAmount, Long maxAmount, List<String> keywords) {
}
