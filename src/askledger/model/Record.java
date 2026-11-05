package askledger;

import java.time.LocalDate;

/**
 * One income or expense record.
 */
public class Record {
    /** Kind of a record. */
    public enum Type {
        /** Money received. */
        INCOME,
        /** Money spent. */
        EXPENSE
    }

    private int id;
    private final LocalDate date;
    private final Type type;
    private final long amount;
    private final String category;
    private final String memo;

    /**
     * Creates a record.
     * @param id unique id (0 if not assigned yet)
     * @param date date of the record
     * @param type income or expense
     * @param amount amount in won (positive)
     * @param category category name
     * @param memo free text memo
     */
    public Record(int id, LocalDate date, Type type, long amount, String category, String memo) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.memo = memo == null ? "" : memo;
    }

    /** @return the id */
    public int getId() { return id; }
    /** @param id the new id, set by RecordManager */
    void setId(int id) { this.id = id; }
    /** @return the date */
    public LocalDate getDate() { return date; }
    /** @return income or expense */
    public Type getType() { return type; }
    /** @return the amount in won */
    public long getAmount() { return amount; }
    /** @return the category name */
    public String getCategory() { return category; }
    /** @return the memo */
    public String getMemo() { return memo; }

    @Override
    public String toString() {
        return date + "  " + type + "  " + String.format("%,d", amount) + " won  " + category + "  " + memo;
    }
}
