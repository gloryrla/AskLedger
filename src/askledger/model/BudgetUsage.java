package askledger.model;

/** 한 카테고리의 한 달 예산 사용 현황 */
public class BudgetUsage {
    private String category;
    private long budget;
    private long spent;
    private double percent;

    public BudgetUsage(String category, long budget, long spent, double percent) {
        this.category = category;
        this.budget = budget;
        this.spent = spent;
        this.percent = percent;
    }

    public String getCategory() {
        return category;
    }

    public long getBudget() {
        return budget;
    }

    public long getSpent() {
        return spent;
    }

    public double getPercent() {
        return percent;
    }

    /** 예산 초과면 true */
    public boolean isOver() {
        return spent > budget;
    }
}
