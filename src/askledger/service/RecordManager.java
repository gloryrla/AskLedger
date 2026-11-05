package askledger;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Keeps the list of all records.
 */
public class RecordManager {
    private final DataStorage storage;
    private List<Record> records = new ArrayList<>();

    /**
     * @param storage used to save after every change
     */
    public RecordManager(DataStorage storage) { this.storage = storage; }

    /**
     * Replaces the record list with the loaded records.
     * @param records loaded records (this list is kept, not copied)
     */
    public void setRecords(List<Record> records) { this.records = records; }

    /**
     * Gives the record a new id, adds it to the list, and saves.
     * @param record new record
     * @return true if it is saved
     */
    public boolean addRecord(Record record) {
        record.setId(records.stream().mapToInt(Record::getId).max().orElse(0) + 1);
        records.add(record);
        return storage.saveAll();
    }

    /**
     * Replaces the record with the given id and saves.
     * @param id id of the record to change
     * @param record new values
     * @return false if the id does not exist
     */
    public boolean updateRecord(int id, Record record) {
        for (int k = 0; k < records.size(); k++) {
            if (records.get(k).getId() == id) {
                record.setId(id);
                records.set(k, record);
                return storage.saveAll();
            }
        }
        return false;
    }

    /**
     * Removes the record with the given id and saves.
     * @param id id of the record
     * @return false if the id does not exist
     */
    public boolean deleteRecord(int id) {
        return records.removeIf(r -> r.getId() == id) && storage.saveAll();
    }

    /**
     * Returns the records that match every condition in the query, sorted by date.
     * A null condition is ignored. Keywords match if any keyword is in the memo or category.
     * @param q search conditions
     * @return matching records
     */
    public List<Record> findRecords(SearchQuery q) {
        return records.stream()
                .filter(r -> q.startDate() == null || !r.getDate().isBefore(q.startDate()))
                .filter(r -> q.endDate() == null || !r.getDate().isAfter(q.endDate()))
                .filter(r -> q.type() == null || r.getType() == q.type())
                .filter(r -> q.categories() == null || q.categories().isEmpty() || q.categories().contains(r.getCategory()))
                .filter(r -> q.minAmount() == null || r.getAmount() >= q.minAmount())
                .filter(r -> q.maxAmount() == null || r.getAmount() <= q.maxAmount())
                .filter(r -> q.keywords() == null || q.keywords().isEmpty() || q.keywords().stream().anyMatch(k ->
                        (r.getMemo() + " " + r.getCategory()).toLowerCase().contains(k.toLowerCase())))
                .sorted(Comparator.comparing(Record::getDate))
                .toList();
    }

    /**
     * Returns the records whose date is in the given month, sorted by date.
     * @param month the month
     * @return records of the month
     */
    public List<Record> getRecordsByMonth(YearMonth month) {
        return findRecords(new SearchQuery(month.atDay(1), month.atEndOfMonth(), null, null, null, null, null));
    }
}
