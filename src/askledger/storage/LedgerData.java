package askledger.storage;

import askledger.model.Record;

import java.util.List;
import java.util.Map;

/**
 * 데이터 파일에 저장되는 전체 데이터.
 * RecordManager, BudgetManager가 같은 리스트/맵 객체를 쓰기 때문에 저장하면 항상 최신 데이터가 저장된다.
 */
public class LedgerData {
    private List<Record> records;
    private Map<String, Long> budgets;
    private List<String> categories;

    public LedgerData(List<Record> records, Map<String, Long> budgets, List<String> categories) {
        this.records = records;
        this.budgets = budgets;
        this.categories = categories;
    }

    public List<Record> getRecords() {
        return records;
    }

    public Map<String, Long> getBudgets() {
        return budgets;
    }

    public List<String> getCategories() {
        return categories;
    }
}
