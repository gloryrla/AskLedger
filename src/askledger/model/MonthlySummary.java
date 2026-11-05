package askledger;

import java.time.YearMonth;
import java.util.List;

/**
 * Totals of one month.
 * @param month the month
 * @param income total income
 * @param expense total expense
 * @param balance income minus expense
 * @param records records of the month
 */
public record MonthlySummary(YearMonth month, long income, long expense, long balance, List<Record> records) {
}
