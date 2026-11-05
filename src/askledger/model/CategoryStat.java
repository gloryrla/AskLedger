package askledger.model;

/** 카테고리별 지출 통계 (금액, 비율) */
public class CategoryStat implements Comparable<CategoryStat> {
    private String category;
    private long amount;
    private double percent;

    public CategoryStat(String category, long amount, double percent) {
        this.category = category;
        this.amount = amount;
        this.percent = percent;
    }

    public String getCategory() {
        return category;
    }

    public long getAmount() {
        return amount;
    }

    public double getPercent() {
        return percent;
    }

    /** 금액 큰 게 앞으로 오게 */
    @Override
    public int compareTo(CategoryStat other) {
        if (amount > other.amount) {
            return -1;
        } else if (amount < other.amount) {
            return 1;
        }
        return 0;
    }
}
