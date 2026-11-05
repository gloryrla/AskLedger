package askledger;

/**
 * Expense of one category in a month.
 * @param category category name
 * @param amount total expense
 * @param percent share of the month's total expense (0-100)
 */
public record CategoryStat(String category, long amount, double percent) {
}
