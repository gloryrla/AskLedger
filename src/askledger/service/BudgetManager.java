package askledger;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the monthly budget of each category.
 */
public class BudgetManager {
    private final DataStorage storage;
    private final RecordManager recordManager;
    private Map<String, Long> budgets = new LinkedHashMap<>();

    /**
     * @param storage used to save after a change
     * @param recordManager used to add up expenses
     */
    public BudgetManager(DataStorage storage, RecordManager recordManager) {
        this.storage = storage;
        this.recordManager = recordManager;
    }

    /**
     * Replaces the budget map with the loaded budgets.
     * @param budgets loaded budgets (this map is kept, not copied)
     */
    public void setBudgets(Map<String, Long> budgets) { this.budgets = budgets; }

    /**
     * Saves the budget of the category.
     * @param category category name
     * @param amount monthly budget
     * @return false if the amount is not positive or saving fails
     */
    public boolean setBudget(String category, long amount) {
        if (amount <= 0) return false;
        budgets.put(category, amount);
        return storage.saveAll();
    }

    /**
     * Checks whether the expenses of the category in the month are over its budget.
     * @param category category name
     * @param month the month
     * @return true if over budget; false if no budget is set
     */
    public boolean checkBudget(String category, YearMonth month) {
        Long budget = budgets.get(category);
        return budget != null && spent(category, month) > budget;
    }

    /**
     * Returns the budget, the spent amount and the usage of each category that has a budget.
     * @param month the month
     * @return usage list in budget order
     */
    public List<BudgetUsage> getUsage(YearMonth month) {
        List<BudgetUsage> list = new ArrayList<>();
        budgets.forEach((c, b) -> {
            long s = spent(c, month);
            list.add(new BudgetUsage(c, b, s, s * 100.0 / b));
        });
        return list;
    }

    private long spent(String category, YearMonth month) {
        return recordManager.getRecordsByMonth(month).stream()
                .filter(r -> r.getType() == Record.Type.EXPENSE && r.getCategory().equals(category))
                .mapToLong(Record::getAmount).sum();
    }
}
