package askledger;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Starts AskLedger.
 * <p>The data file is {@code askledger_data.json} in the current folder, or the path given by the
 * system property {@code askledger.data}.</p>
 */
public class AskLedgerApp {
    private AskLedgerApp() {}

    /**
     * Loads the data file, creates the objects and shows the main window.
     * @param args not used
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        DataStorage storage = new DataStorage(Path.of(System.getProperty("askledger.data", "askledger_data.json")));
        LedgerData data;
        try {
            data = storage.loadAll();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "The data file could not be read:\n" + e.getMessage(),
                    "AskLedger", JOptionPane.ERROR_MESSAGE);
            return; // do not start with empty data, or the next save would overwrite the file
        }
        RecordManager recordManager = new RecordManager(storage);
        recordManager.setRecords(data.records());
        BudgetManager budgetManager = new BudgetManager(storage, recordManager);
        budgetManager.setBudgets(data.budgets());
        SearchService searchService = new SearchService(new LLMClient(data.categories()),
                new RuleParser(data.categories()), recordManager, data.categories());
        StatisticsService statisticsService = new StatisticsService(recordManager);
        SwingUtilities.invokeLater(() ->
                new MainFrame(recordManager, budgetManager, searchService, statisticsService, data.categories()).showWindow());
    }
}
