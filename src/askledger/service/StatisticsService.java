package askledger;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates monthly totals and category statistics.
 */
public class StatisticsService {
    private final RecordManager recordManager;

    /**
     * @param recordManager source of records
     */
    public StatisticsService(RecordManager recordManager) { this.recordManager = recordManager; }

    /**
     * Returns the totals of the month.
     * @param month the month
     * @return income, expense, balance and records
     */
    public MonthlySummary getMonthlySummary(YearMonth month) {
        MonthlySummary t = calcTotals(recordManager.getRecordsByMonth(month));
        return new MonthlySummary(month, t.income(), t.expense(), t.balance(), t.records());
    }

    /**
     * Adds up income and expenses and calculates the balance. All zeros if the list is empty.
     * @param records records to add up
     * @return totals (month is null)
     */
    public MonthlySummary calcTotals(List<Record> records) {
        long in = records.stream().filter(r -> r.getType() == Record.Type.INCOME).mapToLong(Record::getAmount).sum();
        long out = records.stream().filter(r -> r.getType() == Record.Type.EXPENSE).mapToLong(Record::getAmount).sum();
        return new MonthlySummary(null, in, out, in - out, records);
    }

    /**
     * Returns the expense statistics of each category in the month.
     * @param month the month
     * @return statistics sorted by amount, largest first
     */
    public List<CategoryStat> getCategoryStats(YearMonth month) {
        return calcPercentages(sumByCategory(recordManager.getRecordsByMonth(month)));
    }

    /**
     * Adds up the expenses of each category. Income is ignored.
     * @param records records to add up
     * @return category to total expense
     */
    public Map<String, Long> sumByCategory(List<Record> records) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Record r : records)
            if (r.getType() == Record.Type.EXPENSE) m.merge(r.getCategory(), r.getAmount(), Long::sum);
        return m;
    }

    /**
     * Calculates each category's share of the total expense.
     * @param totals category to total expense
     * @return statistics sorted by amount, largest first; empty if there are no expenses
     */
    public List<CategoryStat> calcPercentages(Map<String, Long> totals) {
        long sum = totals.values().stream().mapToLong(Long::longValue).sum();
        if (sum == 0) return List.of();
        return totals.entrySet().stream()
                .map(e -> new CategoryStat(e.getKey(), e.getValue(), e.getValue() * 100.0 / sum))
                .sorted(Comparator.comparingLong(CategoryStat::amount).reversed())
                .toList();
    }
}
