package askledger;

/**
 * Budget usage of one category in a month.
 * @param category category name
 * @param budget monthly budget
 * @param spent expense in the month
 * @param percent spent / budget * 100
 */
public record BudgetUsage(String category, long budget, long spent, double percent) {
    /** @return true if the spent amount is over the budget */
    public boolean isOver() { return spent > budget; }
}
