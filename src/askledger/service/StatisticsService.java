package askledger.service;

import askledger.model.CategoryStat;
import askledger.model.MonthlySummary;
import askledger.model.Record;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 월별 합계와 카테고리별 통계를 계산하는 클래스 */
public class StatisticsService {
    private RecordManager recordManager;

    public StatisticsService(RecordManager recordManager) {
        this.recordManager = recordManager;
    }

    /** 월별 수입/지출/잔액 계산 */
    public MonthlySummary getMonthlySummary(YearMonth month) {
        List<Record> records = recordManager.getRecordsByMonth(month);
        long income = 0;
        long expense = 0;
        for (Record r : records) {
            if (r.getType() == Record.Type.INCOME) {
                income += r.getAmount();
            } else {
                expense += r.getAmount();
            }
        }
        return new MonthlySummary(month, income, expense, income - expense, records);
    }

    /** 카테고리별 지출 합계와 비율(%), 금액 큰 순으로 정렬 */
    public List<CategoryStat> getCategoryStats(YearMonth month) {
        Map<String, Long> totals = new HashMap<>();
        long sum = 0;
        for (Record r : recordManager.getRecordsByMonth(month)) {
            if (r.getType() == Record.Type.EXPENSE) {
                String c = r.getCategory();
                if (totals.containsKey(c)) {
                    totals.put(c, totals.get(c) + r.getAmount());
                } else {
                    totals.put(c, r.getAmount());
                }
                sum += r.getAmount();
            }
        }

        List<CategoryStat> list = new ArrayList<>();
        if (sum == 0) {
            return list;
        }
        for (String category : totals.keySet()) {
            long amount = totals.get(category);
            list.add(new CategoryStat(category, amount, amount * 100.0 / sum));
        }
        Collections.sort(list);
        return list;
    }
}
