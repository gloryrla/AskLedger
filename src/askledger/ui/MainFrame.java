package askledger.ui;

import askledger.model.BudgetUsage;
import askledger.model.CategoryStat;
import askledger.model.MonthlySummary;
import askledger.model.Record;
import askledger.model.SearchQuery;
import askledger.model.SearchResult;
import askledger.service.BudgetManager;
import askledger.service.RecordManager;
import askledger.service.SearchService;
import askledger.service.StatisticsService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** 메인 화면 (기록, 월별, 통계, 예산 탭) */
public class MainFrame extends JFrame implements ActionListener {
    private RecordManager recordManager;
    private BudgetManager budgetManager;
    private SearchService searchService;
    private StatisticsService statisticsService;
    private List<String> categories;

    private YearMonth currentMonth = YearMonth.now();
    private SearchQuery lastQuery = null;   // 마지막 검색 조건 (null이면 이번 달 기록을 보여줌)
    private List<Record> shownRecords = new ArrayList<>();   // 지금 기록 표에 보이는 기록들

    // 기록 탭
    private JTextField questionField = new JTextField(30);
    private JButton searchButton = new JButton("Search");
    private JButton applyButton = new JButton("Apply");
    private JButton addButton = new JButton("Add");
    private JButton editButton = new JButton("Edit");
    private JButton deleteButton = new JButton("Delete");
    private JButton saveBudgetButton = new JButton("Save");
    private JTextField fromField = new JTextField(8);
    private JTextField toField = new JTextField(8);
    private JComboBox<String> typeBox = new JComboBox<>(new String[]{"All", "Income", "Expense"});
    private JComboBox<String> categoryBox = new JComboBox<>();
    private JTextField minField = new JTextField(6);
    private JTextField maxField = new JTextField(6);
    private JTextField keywordField = new JTextField(10);
    private JLabel infoLabel = new JLabel();
    private JLabel summaryLabel = new JLabel();
    private DefaultTableModel recordModel = makeModel(new String[]{"Date", "Type", "Amount", "Category", "Memo"});
    private JTable recordTable = new JTable(recordModel);

    // 월별 탭
    private JLabel incomeLabel = new JLabel();
    private JLabel expenseLabel = new JLabel();
    private JLabel balanceLabel = new JLabel();
    private DefaultTableModel monthModel = makeModel(new String[]{"Date", "Type", "Amount", "Category", "Memo"});

    // 통계 탭
    private DefaultTableModel statModel = makeModel(new String[]{"Category", "Amount", "Share (%)"});
    private JLabel statLabel = new JLabel();

    // 예산 탭
    private JComboBox<String> budgetCategoryBox = new JComboBox<>();
    private JTextField budgetField = new JTextField(10);
    private DefaultTableModel usageModel = makeModel(new String[]{"Category", "Budget", "Spent", "Usage (%)", "Status"});

    // 월별, 통계, 예산 탭의 월 표시 라벨
    private JLabel[] monthLabels = {new JLabel(), new JLabel(), new JLabel()};

    public MainFrame(RecordManager recordManager, BudgetManager budgetManager, SearchService searchService,
                     StatisticsService statisticsService, List<String> categories) {
        super("AskLedger");
        this.recordManager = recordManager;
        this.budgetManager = budgetManager;
        this.searchService = searchService;
        this.statisticsService = statisticsService;
        this.categories = categories;

        categoryBox.addItem("All");
        for (String c : categories) {
            categoryBox.addItem(c);
            budgetCategoryBox.addItem(c);
        }

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Records", makeRecordsTab());
        tabs.addTab("Monthly", makeMonthlyTab());
        tabs.addTab("Statistics", makeStatisticsTab());
        tabs.addTab("Budget", makeBudgetTab());
        add(tabs);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    // 기록 탭 화면 만들기
    private JPanel makeRecordsTab() {
        questionField.addActionListener(this);
        searchButton.addActionListener(this);
        applyButton.addActionListener(this);
        addButton.addActionListener(this);
        editButton.addActionListener(this);
        deleteButton.addActionListener(this);

        JPanel askPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        askPanel.add(new JLabel("Ask:"));
        askPanel.add(questionField);
        askPanel.add(searchButton);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("From"));
        row1.add(fromField);
        row1.add(new JLabel("To"));
        row1.add(toField);
        row1.add(new JLabel("Type"));
        row1.add(typeBox);
        row1.add(new JLabel("Category"));
        row1.add(categoryBox);
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Min amount"));
        row2.add(minField);
        row2.add(new JLabel("Max amount"));
        row2.add(maxField);
        row2.add(new JLabel("Keyword"));
        row2.add(keywordField);
        row2.add(applyButton);
        JPanel conditionPanel = new JPanel(new GridLayout(2, 1));
        conditionPanel.setBorder(BorderFactory.createTitledBorder("Understood conditions"));
        conditionPanel.add(row1);
        conditionPanel.add(row2);

        JPanel top = new JPanel(new BorderLayout());
        top.add(askPanel, BorderLayout.NORTH);
        top.add(conditionPanel, BorderLayout.CENTER);
        top.add(infoLabel, BorderLayout.SOUTH);

        JPanel buttons = new JPanel();
        buttons.add(addButton);
        buttons.add(editButton);
        buttons.add(deleteButton);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(summaryLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        recordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(recordTable), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // 월별 탭 화면 만들기
    private JPanel makeMonthlyTab() {
        JPanel totals = new JPanel(new GridLayout(1, 3));
        totals.add(incomeLabel);
        totals.add(expenseLabel);
        totals.add(balanceLabel);
        JPanel top = new JPanel(new BorderLayout());
        top.add(makeMonthBar(monthLabels[0]), BorderLayout.NORTH);
        top.add(totals, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(monthModel)), BorderLayout.CENTER);
        return panel;
    }

    // 통계 탭 화면 만들기
    private JPanel makeStatisticsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(makeMonthBar(monthLabels[1]), BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(statModel)), BorderLayout.CENTER);
        panel.add(statLabel, BorderLayout.SOUTH);
        return panel;
    }

    // 예산 탭 화면 만들기
    private JPanel makeBudgetTab() {
        saveBudgetButton.addActionListener(this);
        JPanel setPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        setPanel.setBorder(BorderFactory.createTitledBorder("Set monthly budget"));
        setPanel.add(new JLabel("Category"));
        setPanel.add(budgetCategoryBox);
        setPanel.add(new JLabel("Amount (won)"));
        setPanel.add(budgetField);
        setPanel.add(saveBudgetButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(setPanel, BorderLayout.NORTH);
        top.add(makeMonthBar(monthLabels[2]), BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(new JTable(usageModel)), BorderLayout.CENTER);
        return panel;
    }

    // "< Prev  2026-11  Next >" 월 이동 바
    private JPanel makeMonthBar(JLabel label) {
        JButton prevButton = new JButton("< Prev");
        JButton nextButton = new JButton("Next >");
        prevButton.setActionCommand("prev");
        nextButton.setActionCommand("next");
        prevButton.addActionListener(this);
        nextButton.addActionListener(this);
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(prevButton);
        bar.add(label);
        bar.add(nextButton);
        return bar;
    }

    // 표 수정 못하게 막음
    private static DefaultTableModel makeModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // 금액에 콤마와 "won" 붙이기
    private static String won(long amount) {
        return String.format("%,d won", amount);
    }

    // 기록 목록을 표에 채운다
    private static void fillRecords(DefaultTableModel model, List<Record> records) {
        model.setRowCount(0);
        for (Record r : records) {
            String type = r.getType() == Record.Type.INCOME ? "Income" : "Expense";
            model.addRow(new Object[]{r.getDate().toString(), type, String.format("%,d", r.getAmount()),
                    r.getCategory(), r.getMemo()});
        }
    }

    /** 버튼을 누르거나 질문 칸에서 엔터를 치면 여기로 온다 */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == questionField || src == searchButton) {
            enterQuestion(questionField.getText());
        } else if (src == applyButton) {
            clickApply();
        } else if (src == addButton) {
            clickAdd();
        } else if (src == editButton) {
            clickEdit();
        } else if (src == deleteButton) {
            clickDelete();
        } else if (src == saveBudgetButton) {
            clickSaveBudget((String) budgetCategoryBox.getSelectedItem(), budgetField.getText());
        } else if (e.getActionCommand().equals("prev")) {
            selectMonth(currentMonth.minusMonths(1));
        } else if (e.getActionCommand().equals("next")) {
            selectMonth(currentMonth.plusMonths(1));
        }
    }

    public void showWindow() {
        refreshTable();
        selectMonth(currentMonth);
        setVisible(true);
    }

    /** 추가 버튼: 빈 입력창을 연다 */
    public void clickAdd() {
        RecordDialog dialog = new RecordDialog(this, recordManager, categories);
        Record record = dialog.showDialog();
        afterSave(record);
    }

    /** 수정 버튼: 선택한 기록이 채워진 입력창을 연다 */
    public void clickEdit() {
        Record selected = getSelectedRecord();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a record first.");
            return;
        }
        RecordDialog dialog = new RecordDialog(this, recordManager, categories);
        Record record = dialog.showDialog(selected);
        afterSave(record);
    }

    // 기록 추가/수정 후 화면을 새로고침하고 예산 초과인지 확인
    private void afterSave(Record record) {
        if (record == null) {
            return;
        }
        refreshTable();
        selectMonth(currentMonth);
        // 지출이면 그 달 예산을 넘었는지 확인해서 경고
        if (record.getType() == Record.Type.EXPENSE) {
            YearMonth month = YearMonth.from(record.getDate());
            if (budgetManager.checkBudget(record.getCategory(), month)) {
                for (BudgetUsage usage : budgetManager.getUsage(month)) {
                    if (usage.getCategory().equals(record.getCategory())) {
                        showBudgetWarning(usage);
                    }
                }
            }
        }
    }

    /** 삭제 버튼: 한 번 더 물어보고 삭제 */
    public void clickDelete() {
        Record selected = getSelectedRecord();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a record first.");
            return;
        }
        if (showConfirm("Do you want to delete this record?\n\n" + selected)) {
            recordManager.deleteRecord(selected.getId());
            refreshTable();
            selectMonth(currentMonth);
        }
    }

    /** 선택된 행의 기록 가져오기 */
    public Record getSelectedRecord() {
        int row = recordTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return shownRecords.get(row);
    }

    /** 삭제 확인창 */
    public boolean showConfirm(String message) {
        int answer = JOptionPane.showConfirmDialog(this, message, "Delete Record", JOptionPane.YES_NO_OPTION);
        return answer == JOptionPane.YES_OPTION;
    }

    /** 기록 표 새로고침: 마지막 검색을 다시 하거나, 없으면 이번 달 기록 */
    public void refreshTable() {
        if (lastQuery != null) {
            SearchResult result = searchService.apply(lastQuery);
            if (result != null) {
                showResults(result);
                return;
            }
        }
        List<Record> records = recordManager.getRecordsByMonth(YearMonth.now());
        showTable(records);
        summaryLabel.setText(YearMonth.now() + ": " + records.size() + " records");
    }

    /** 예산, 쓴 돈, 초과 금액을 경고창으로 보여준다 */
    public void showBudgetWarning(BudgetUsage usage) {
        String message = usage.getCategory() + " budget exceeded\n\n"
                + "Budget: " + won(usage.getBudget()) + "\n"
                + "Spent: " + won(usage.getSpent()) + "\n"
                + "Over by: " + won(usage.getSpent() - usage.getBudget());
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /** 질문 검색. 빈 질문이면 검색을 지운다 */
    public void enterQuestion(String question) {
        if (question.trim().isEmpty()) {
            lastQuery = null;
            infoLabel.setText("");
            refreshTable();
            return;
        }
        SearchResult result = searchService.search(question);
        showResults(result);
    }

    /** 조건 입력칸들을 읽어서 SearchQuery로 만든다 */
    public SearchQuery readConditions() {
        LocalDate from = null;
        if (!fromField.getText().trim().isEmpty()) {
            from = LocalDate.parse(fromField.getText().trim());
        }
        LocalDate to = null;
        if (!toField.getText().trim().isEmpty()) {
            to = LocalDate.parse(toField.getText().trim());
        }
        Record.Type type = null;
        if (typeBox.getSelectedItem().equals("Income")) {
            type = Record.Type.INCOME;
        } else if (typeBox.getSelectedItem().equals("Expense")) {
            type = Record.Type.EXPENSE;
        }
        List<String> cats = new ArrayList<>();
        if (!categoryBox.getSelectedItem().equals("All")) {
            cats.add((String) categoryBox.getSelectedItem());
        }
        Long min = null;
        if (!minField.getText().trim().isEmpty()) {
            min = Long.parseLong(minField.getText().trim().replace(",", ""));
        }
        Long max = null;
        if (!maxField.getText().trim().isEmpty()) {
            max = Long.parseLong(maxField.getText().trim().replace(",", ""));
        }
        List<String> keywords = new ArrayList<>();
        for (String k : keywordField.getText().split(",")) {
            if (!k.trim().isEmpty()) {
                keywords.add(k.trim());
            }
        }
        return new SearchQuery(from, to, type, cats, min, max, keywords);
    }

    /** 적용 버튼: 사용자가 고친 조건으로 다시 검색 (LLM 안 씀) */
    public void clickApply() {
        SearchQuery query;
        try {
            query = readConditions();
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Enter dates as yyyy-MM-dd.");
            return;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Amounts must be numbers.");
            return;
        }
        SearchResult result = searchService.apply(query);
        if (result == null) {
            JOptionPane.showMessageDialog(this, "Start date must not be after end date, and amounts must not be negative.");
            return;
        }
        showResults(result);
    }

    /** 알아들은 조건, 찾은 기록, 개수, 합계를 화면에 보여준다 */
    public void showResults(SearchResult result) {
        SearchQuery q = result.getQuery();
        lastQuery = q;

        fromField.setText(q.getStartDate() == null ? "" : q.getStartDate().toString());
        toField.setText(q.getEndDate() == null ? "" : q.getEndDate().toString());
        if (q.getType() == Record.Type.INCOME) {
            typeBox.setSelectedItem("Income");
        } else if (q.getType() == Record.Type.EXPENSE) {
            typeBox.setSelectedItem("Expense");
        } else {
            typeBox.setSelectedItem("All");
        }
        if (q.getCategories() == null || q.getCategories().isEmpty()) {
            categoryBox.setSelectedItem("All");
        } else {
            categoryBox.setSelectedItem(q.getCategories().get(0));
        }
        minField.setText(q.getMinAmount() == null ? "" : q.getMinAmount().toString());
        maxField.setText(q.getMaxAmount() == null ? "" : q.getMaxAmount().toString());
        keywordField.setText(q.getKeywords() == null ? "" : String.join(", ", q.getKeywords()));

        showTable(result.getRecords());
        summaryLabel.setText(result.getCount() + " records, total " + won(result.getTotal()));

        String info = "";
        if (result.getRecords().isEmpty()) {
            info += "No results. ";
        }
        if (result.isUsedRuleParser()) {
            info += "Simple search was used (LLM not available).";
        }
        infoLabel.setText(info);
    }

    /** 월별, 통계, 예산 탭에 보여줄 달을 바꾼다 */
    public void selectMonth(YearMonth month) {
        currentMonth = month;
        for (JLabel label : monthLabels) {
            label.setText(month.toString());
        }
        showSummary(statisticsService.getMonthlySummary(month));
        showStats(statisticsService.getCategoryStats(month));
        showUsage(budgetManager.getUsage(month));
    }

    /** 월 수입, 지출, 잔액과 그 달 기록을 보여준다 */
    public void showSummary(MonthlySummary summary) {
        incomeLabel.setText("  Total income: " + won(summary.getIncome()));
        expenseLabel.setText("  Total expense: " + won(summary.getExpense()));
        balanceLabel.setText("  Balance: " + won(summary.getBalance()));
        fillRecords(monthModel, summary.getRecords());
    }

    /** 카테고리별 금액과 비율. 지출이 없으면 "No expenses" */
    public void showStats(List<CategoryStat> stats) {
        statModel.setRowCount(0);
        long total = 0;
        for (CategoryStat s : stats) {
            statModel.addRow(new Object[]{s.getCategory(), String.format("%,d", s.getAmount()),
                    String.format("%.1f", s.getPercent())});
            total += s.getAmount();
        }
        if (stats.isEmpty()) {
            statLabel.setText("No expenses");
        } else {
            statLabel.setText("Total expense: " + won(total));
        }
    }

    /** 금액 체크 */
    public boolean validateAmount(String text) {
        try {
            long amount = Long.parseLong(text.trim().replace(",", ""));
            if (amount > 0) {
                return true;
            }
        } catch (NumberFormatException e) {
        }
        JOptionPane.showMessageDialog(this, "The amount must be a positive number.");
        return false;
    }

    /** 예산 저장 버튼 */
    public void clickSaveBudget(String category, String amount) {
        if (!validateAmount(amount)) {
            return;
        }
        budgetManager.setBudget(category, Long.parseLong(amount.trim().replace(",", "")));
        budgetField.setText("");
        showUsage(budgetManager.getUsage(currentMonth));
    }

    /** 카테고리별 예산 사용 현황 표 */
    public void showUsage(List<BudgetUsage> usageList) {
        usageModel.setRowCount(0);
        for (BudgetUsage u : usageList) {
            String status = u.isOver() ? "Over budget" : "";
            usageModel.addRow(new Object[]{u.getCategory(), won(u.getBudget()), won(u.getSpent()),
                    String.format("%.0f", u.getPercent()), status});
        }
    }

    public void showTable(List<Record> records) {
        shownRecords = records;
        fillRecords(recordModel, records);
    }
}
