package askledger.model;

import java.time.LocalDate;
import java.util.List;

/** 검색 조건. null(또는 빈 리스트)인 조건은 무시한다 */
public class SearchQuery {
    private LocalDate startDate;
    private LocalDate endDate;
    private Record.Type type;
    private List<String> categories;
    private Long minAmount;
    private Long maxAmount;
    private List<String> keywords;

    public SearchQuery(LocalDate startDate, LocalDate endDate, Record.Type type, List<String> categories,
                       Long minAmount, Long maxAmount, List<String> keywords) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
        this.categories = categories;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.keywords = keywords;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Record.Type getType() {
        return type;
    }

    public List<String> getCategories() {
        return categories;
    }

    public Long getMinAmount() {
        return minAmount;
    }

    public Long getMaxAmount() {
        return maxAmount;
    }

    public List<String> getKeywords() {
        return keywords;
    }
}
