package askledger.model;

import java.time.YearMonth;
import java.util.List;

/** 한 달 합계 (수입, 지출, 잔액, 기록 목록) */
public class MonthlySummary {
    private YearMonth month;
    private long income;
    private long expense;
    private long balance;
    private List<Record> records;

    public MonthlySummary(YearMonth month, long income, long expense, long balance, List<Record> records) {
        this.month = month;
        this.income = income;
        this.expense = expense;
        this.balance = balance;
        this.records = records;
    }

    public YearMonth getMonth() {
        return month;
    }

    public long getIncome() {
        return income;
    }

    public long getExpense() {
        return expense;
    }

    public long getBalance() {
        return balance;
    }

    public List<Record> getRecords() {
        return records;
    }
}
