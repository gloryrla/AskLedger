package askledger.service;

import askledger.model.BudgetUsage;
import askledger.model.Record;
import askledger.storage.DataStorage;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 카테고리별 월 예산을 관리하는 클래스 */
public class BudgetManager {
    private DataStorage storage;
    private RecordManager recordManager;
    private Map<String, Long> budgets = new HashMap<>();   // 카테고리 이름 -> 예산 금액

    public BudgetManager(DataStorage storage, RecordManager recordManager) {
        this.storage = storage;
        this.recordManager = recordManager;
    }

    public void setBudgets(Map<String, Long> budgets) {
        this.budgets = budgets;
    }

    /** 예산을 저장한다. 금액이 0 이하면 저장 안 함 */
    public boolean setBudget(String category, long amount) {
        if (amount <= 0) {
            return false;
        }
        budgets.put(category, amount);
        return storage.saveAll();
    }

    /** 해당 달 지출이 예산을 넘었는지 확인 */
    public boolean checkBudget(String category, YearMonth month) {
        Long budget = budgets.get(category);
        if (budget == null) {
            return false;
        }
        return getSpent(category, month) > budget;
    }

    /** 예산이 있는 카테고리마다 예산, 쓴 돈, 사용률(%)을 구한다 */
    public List<BudgetUsage> getUsage(YearMonth month) {
        List<BudgetUsage> list = new ArrayList<>();
        for (String category : budgets.keySet()) {
            long budget = budgets.get(category);
            long spent = getSpent(category, month);
            list.add(new BudgetUsage(category, budget, spent, spent * 100.0 / budget));
        }
        return list;
    }

    // 해당 달에 그 카테고리에서 쓴 돈 합계
    private long getSpent(String category, YearMonth month) {
        long sum = 0;
        for (Record r : recordManager.getRecordsByMonth(month)) {
            if (r.getType() == Record.Type.EXPENSE && r.getCategory().equals(category)) {
                sum += r.getAmount();
            }
        }
        return sum;
    }
}
