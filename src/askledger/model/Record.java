package askledger.model;

import java.time.LocalDate;

/** 수입 또는 지출 기록 하나 */
public class Record implements Comparable<Record> {
    /** 기록 종류 (수입/지출) */
    public enum Type { INCOME, EXPENSE }

    private int id;
    private LocalDate date;
    private Type type;
    private long amount;
    private String category;
    private String memo;

    public Record(int id, LocalDate date, Type type, long amount, String category, String memo) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.amount = amount;
        this.category = category;
        if (memo == null) {
            memo = "";
        }
        this.memo = memo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public Type getType() {
        return type;
    }

    public long getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getMemo() {
        return memo;
    }

    /** Collections.sort 할 때 날짜순으로 정렬하려고 */
    @Override
    public int compareTo(Record other) {
        return date.compareTo(other.date);
    }

    @Override
    public String toString() {
        String typeName = "Expense";
        if (type == Type.INCOME) {
            typeName = "Income";
        }
        return date + "  " + typeName + "  " + String.format("%,d", amount) + " won  " + category + "  " + memo;
    }
}
