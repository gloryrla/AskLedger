package askledger;

import askledger.service.BudgetManager;
import askledger.service.LLMClient;
import askledger.service.RecordManager;
import askledger.service.RuleParser;
import askledger.service.SearchService;
import askledger.service.StatisticsService;
import askledger.storage.DataStorage;
import askledger.storage.LedgerData;
import askledger.ui.MainFrame;

import javax.swing.JOptionPane;

/** 프로그램 시작 클래스 (main 메서드가 있음) */
public class AskLedgerApp {
    public static void main(String[] args) {
        DataStorage storage = new DataStorage("askledger_data.json");
        LedgerData data;
        try {
            data = storage.loadAll();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "The data file could not be read.");
            return;
        }

        RecordManager recordManager = new RecordManager(storage);
        recordManager.setRecords(data.getRecords());
        BudgetManager budgetManager = new BudgetManager(storage, recordManager);
        budgetManager.setBudgets(data.getBudgets());

        LLMClient llmClient = new LLMClient(data.getCategories());
        RuleParser ruleParser = new RuleParser(data.getCategories());
        SearchService searchService = new SearchService(llmClient, ruleParser, recordManager, data.getCategories());
        StatisticsService statisticsService = new StatisticsService(recordManager);

        MainFrame frame = new MainFrame(recordManager, budgetManager, searchService, statisticsService, data.getCategories());
        frame.showWindow();
    }
}
