package askledger;

import java.util.List;
import java.util.Map;

/**
 * Everything saved in the data file. The lists and the map are shared with
 * RecordManager and BudgetManager, so DataStorage always saves the latest data.
 * @param records all records
 * @param budgets monthly budget of each category
 * @param categories known category names
 */
public record LedgerData(List<Record> records, Map<String, Long> budgets, List<String> categories) {
}
