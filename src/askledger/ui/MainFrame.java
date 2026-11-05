package askledger;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The main window. Receives user actions, asks the other objects to do the work, and shows the results.
 * It has four tabs: Records, Monthly, Statistics and Budget.
 */
public class MainFrame extends JFrame {
    private final RecordManager recordManager;
    private final BudgetManager budgetManager;
    private final SearchService searchService;
    private final StatisticsService statisticsService;
    private final List<String> categories;

    private YearMonth currentMonth = YearMonth.now();
    private SearchQuery lastQuery;
    private final List<JLabel> monthLabels = new ArrayList<>();

    // Records tab
    private final JTextField askField = new JTextField(36);
    private final JButton searchButton = new JButton("Search");
    private final JTextField fromField = new JTextField(8);
    private final JTextField toField = new JTextField(8);
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"All", "Income", "Expense"});
    private final JComboBox<String> catBox;
    private final JTextField minField = new JTextField(6);
    private final JTextField maxField = new JTextField(6);
    private final JTextField keywordField = new JTextField(10);
    private final JLabel infoLabel = new JLabel(" ");
    private final JLabel summaryLabel = new JLabel(" ");
    private final RecordTableModel recordModel = new RecordTableModel();
    private final JTable recordTable = new JTable(recordModel);

    // Monthly tab
    private final JLabel incomeLabel = box();
    private final JLabel expenseLabel = box();
    private final JLabel balanceLabel = box();
    private final RecordTableModel monthModel = new RecordTableModel();

    // Statistics tab
    private final DefaultTableModel statModel = new DefaultTableModel(new String[]{"Category", "Amount", "Share (%)", "Chart"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Class<?> getColumnClass(int c) { return new Class<?>[]{String.class, Long.class, String.class, Integer.class}[c]; }
    };
    private final JLabel statTotalLabel = new JLabel(" ");

    // Budget tab
    private final JComboBox<String> budgetCatBox;
    private final JTextField budgetAmountField = new JTextField(10);
    private final JPanel usagePanel = new JPanel(new GridBagLayout());

    /**
     * @param recordManager record list
     * @param budgetManager budgets
     * @param searchService search by question
     * @param statisticsService monthly totals and category statistics
     * @param categories category names
     */
    public MainFrame(RecordManager recordManager, BudgetManager budgetManager, SearchService searchService,
                     StatisticsService statisticsService, List<String> categories) {
        super("AskLedger");
        this.recordManager = recordManager;
        this.budgetManager = budgetManager;
        this.searchService = searchService;
        this.statisticsService = statisticsService;
        this.categories = categories;
        List<String> all = new ArrayList<>(List.of("All"));
        all.addAll(categories);
        catBox = new JComboBox<>(all.toArray(new String[0]));
        budgetCatBox = new JComboBox<>(categories.toArray(new String[0]));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Records", recordsTab());
        tabs.addTab("Monthly", monthlyTab());
        tabs.addTab("Statistics", statisticsTab());
        tabs.addTab("Budget", budgetTab());
        add(tabs);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------- layout

    private JPanel recordsTab() {
        JPanel ask = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ask.add(new JLabel("Ask:"));
        ask.add(askField);
        ask.add(searchButton);
        askField.setToolTipText("e.g. How much did I spend on chicken last month?");
        askField.addActionListener(e -> enterQuestion(askField.getText()));
        searchButton.addActionListener(e -> enterQuestion(askField.getText()));

        JButton apply = new JButton("Apply");
        apply.addActionListener(e -> clickApply());
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        for (Object o : new Object[]{"From", fromField, "To", toField, "Type", typeBox, "Category", catBox})
            row1.add(o instanceof String s ? new JLabel(s) : (Component) o);
        for (Object o : new Object[]{"Min amount", minField, "Max amount", maxField, "Keyword", keywordField, apply})
            row2.add(o instanceof String s ? new JLabel(s) : (Component) o);
        JPanel cond = new JPanel(new GridLayout(2, 1));
        cond.setBorder(BorderFactory.createTitledBorder("Understood conditions"));
        cond.add(row1);
        cond.add(row2);
        infoLabel.setForeground(new Color(0x1565C0));

        JPanel north = new JPanel(new BorderLayout());
        north.add(ask, BorderLayout.NORTH);
        north.add(cond, BorderLayout.CENTER);
        north.add(infoLabel, BorderLayout.SOUTH);

        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton del = new JButton("Delete");
        add.addActionListener(e -> clickAdd());
        edit.addActionListener(e -> clickEdit());
        del.addActionListener(e -> clickDelete());
        JPanel buttons = new JPanel();
        buttons.add(add);
        buttons.add(edit);
        buttons.add(del);
        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        south.add(summaryLabel, BorderLayout.WEST);
        south.add(buttons, BorderLayout.EAST);

        setupTable(recordTable);
        JPanel p = new JPanel(new BorderLayout());
        p.add(north, BorderLayout.NORTH);
        p.add(new JScrollPane(recordTable), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel monthlyTab() {
        JPanel boxes = new JPanel(new GridLayout(1, 3, 10, 0));
        boxes.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        boxes.add(titled("Total income", incomeLabel));
        boxes.add(titled("Total expense", expenseLabel));
        boxes.add(titled("Balance", balanceLabel));
        JPanel north = new JPanel(new BorderLayout());
        north.add(monthBar(), BorderLayout.NORTH);
        north.add(boxes, BorderLayout.CENTER);
        JTable t = new JTable(monthModel);
        setupTable(t);
        JPanel p = new JPanel(new BorderLayout());
        p.add(north, BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        return p;
    }

    private JPanel statisticsTab() {
        JTable t = new JTable(statModel);
        setupTable(t);
        t.setDefaultRenderer(Integer.class, (table, v, sel, focus, r, c) -> {
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue((Integer) v);
            return bar;
        });
        statTotalLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JPanel p = new JPanel(new BorderLayout());
        p.add(monthBar(), BorderLayout.NORTH);
        p.add(new JScrollPane(t), BorderLayout.CENTER);
        p.add(statTotalLabel, BorderLayout.SOUTH);
        return p;
    }

    private JPanel budgetTab() {
        JPanel set = new JPanel(new FlowLayout(FlowLayout.LEFT));
        set.setBorder(BorderFactory.createTitledBorder("Set monthly budget"));
        JButton save = new JButton("Save");
        save.addActionListener(e -> clickSaveBudget((String) budgetCatBox.getSelectedItem(), budgetAmountField.getText()));
        set.add(new JLabel("Category"));
        set.add(budgetCatBox);
        set.add(new JLabel("Amount (won)"));
        set.add(budgetAmountField);
        set.add(save);
        JPanel north = new JPanel(new BorderLayout());
        north.add(set, BorderLayout.NORTH);
        north.add(monthBar(), BorderLayout.CENTER);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(usagePanel, BorderLayout.NORTH);
        usagePanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel p = new JPanel(new BorderLayout());
        p.add(north, BorderLayout.NORTH);
        p.add(new JScrollPane(wrap), BorderLayout.CENTER);
        return p;
    }

    /** A Prev / month / Next bar. All bars share one selected month. */
    private JPanel monthBar() {
        JButton prev = new JButton("< Prev");
        JButton next = new JButton("Next >");
        JLabel label = new JLabel(currentMonth.toString());
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        monthLabels.add(label);
        prev.addActionListener(e -> selectMonth(currentMonth.minusMonths(1)));
        next.addActionListener(e -> selectMonth(currentMonth.plusMonths(1)));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        bar.add(prev);
        bar.add(label);
        bar.add(next);
        return bar;
    }

    private static JLabel box() {
        JLabel l = new JLabel("0 won", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 18f));
        return l;
    }

    private static JPanel titled(String title, JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(c);
        p.setPreferredSize(new Dimension(150, 70));
        return p;
    }

    private static void setupTable(JTable t) {
        t.setAutoCreateRowSorter(true);
        t.setRowHeight(24);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer money = new DefaultTableCellRenderer() {
            @Override protected void setValue(Object v) {
                setText(v == null ? "" : String.format("%,d", (Long) v));
                setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
            }
        };
        money.setHorizontalAlignment(SwingConstants.RIGHT);
        t.setDefaultRenderer(Long.class, money);
    }

    private static String won(long v) { return String.format("%,d won", v); }

    // --------------------------------------------------------------- actions

    /** Shows the main window and the records of the current month. */
    public void showWindow() {
        refreshTable();
        selectMonth(currentMonth);
        setVisible(true);
    }

    /** Called when the Add button is clicked. Opens an empty RecordDialog. */
    public void clickAdd() {
        Record r = new RecordDialog(this, recordManager, categories).showDialog();
        afterSave(r);
    }

    /** Called when the Edit button is clicked. Opens RecordDialog with the selected record. */
    public void clickEdit() {
        Record sel = getSelectedRecord();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select a record first."); return; }
        afterSave(new RecordDialog(this, recordManager, categories).showDialog(sel));
    }

    private void afterSave(Record r) {
        if (r == null) return;
        refreshTable();
        selectMonth(currentMonth);
        YearMonth m = YearMonth.from(r.getDate());
        if (r.getType() == Record.Type.EXPENSE && budgetManager.checkBudget(r.getCategory(), m)) {
            budgetManager.getUsage(m).stream().filter(u -> u.category().equals(r.getCategory()))
                    .findFirst().ifPresent(this::showBudgetWarning);
        }
    }

    /** Called when the Delete button is clicked. Asks for confirmation and deletes the selected record. */
    public void clickDelete() {
        Record sel = getSelectedRecord();
        if (sel == null) { JOptionPane.showMessageDialog(this, "Select a record first."); return; }
        if (showConfirm("Do you want to delete this record?\n\n" + sel)) {
            recordManager.deleteRecord(sel.getId());
            refreshTable();
            selectMonth(currentMonth);
        }
    }

    /** @return the record of the selected table row, or null */
    public Record getSelectedRecord() {
        int row = recordTable.getSelectedRow();
        return row < 0 ? null : recordModel.rows.get(recordTable.convertRowIndexToModel(row));
    }

    /**
     * Shows a Yes/No dialog.
     * @param message question to show
     * @return true if the user clicks Yes
     */
    public boolean showConfirm(String message) {
        return JOptionPane.showConfirmDialog(this, message, "Delete Record", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** Reloads the records table: the last search again, or the records of the current month. */
    public void refreshTable() {
        if (lastQuery != null) {
            showResults(searchService.apply(lastQuery));
        } else {
            List<Record> rows = recordManager.getRecordsByMonth(YearMonth.now());
            showTable(rows);
            summaryLabel.setText(YearMonth.now() + ": " + rows.size() + " records");
        }
    }

    /**
     * Shows a warning with the budget, the spent amount and the exceeded amount.
     * @param u usage of the category
     */
    public void showBudgetWarning(BudgetUsage u) {
        JOptionPane.showMessageDialog(this, budgetWarningText(u), "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * @param u usage of the category
     * @return text of the budget warning
     */
    static String budgetWarningText(BudgetUsage u) {
        return u.category() + " budget exceeded\n\nBudget: " + won(u.budget()) + "\nSpent: " + won(u.spent())
                + "\nOver by: " + won(u.spent() - u.budget());
    }

    /**
     * Called when the user presses Enter in the Ask field or clicks Search.
     * The LLM call runs in the background so the window does not freeze. An empty question clears the search.
     * @param question the user's question
     */
    public void enterQuestion(String question) {
        if (question.isBlank()) {
            lastQuery = null;
            infoLabel.setText(" ");
            refreshTable();
            return;
        }
        searchButton.setEnabled(false);
        infoLabel.setText("Searching...");
        new SwingWorker<SearchResult, Void>() {
            @Override protected SearchResult doInBackground() { return searchService.search(question); }
            @Override protected void done() {
                searchButton.setEnabled(true);
                try {
                    showResults(get());
                } catch (Exception e) {
                    infoLabel.setText("Search failed: " + e.getMessage());
                }
            }
        }.execute();
    }

    /**
     * Reads the condition panel.
     * @return the conditions
     * @throws DateTimeParseException if a date is not yyyy-MM-dd
     * @throws NumberFormatException if an amount is not a number
     */
    public SearchQuery readConditions() {
        String type = (String) typeBox.getSelectedItem();
        String cat = (String) catBox.getSelectedItem();
        String kw = keywordField.getText().trim();
        return new SearchQuery(
                fromField.getText().isBlank() ? null : LocalDate.parse(fromField.getText().trim()),
                toField.getText().isBlank() ? null : LocalDate.parse(toField.getText().trim()),
                "All".equals(type) ? null : Record.Type.valueOf(type.toUpperCase()),
                "All".equals(cat) ? List.of() : List.of(cat),
                minField.getText().isBlank() ? null : Long.parseLong(minField.getText().trim().replace(",", "")),
                maxField.getText().isBlank() ? null : Long.parseLong(maxField.getText().trim().replace(",", "")),
                kw.isEmpty() ? List.of() : Arrays.asList(kw.split("[,\\s]+")));
    }

    /** Called when Apply is clicked. Searches again with the corrected conditions, without the LLM. */
    public void clickApply() {
        try {
            showResults(searchService.apply(readConditions()));
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Enter dates as yyyy-MM-dd.", "Invalid condition", JOptionPane.ERROR_MESSAGE);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e instanceof NumberFormatException ? "Amounts must be numbers." : e.getMessage(),
                    "Invalid condition", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Shows the understood conditions, the matching records, the count and the total.
     * @param r search result
     */
    public void showResults(SearchResult r) {
        SearchQuery q = r.query();
        lastQuery = q;
        fromField.setText(q.startDate() == null ? "" : q.startDate().toString());
        toField.setText(q.endDate() == null ? "" : q.endDate().toString());
        typeBox.setSelectedItem(q.type() == null ? "All" : q.type() == Record.Type.INCOME ? "Income" : "Expense");
        catBox.setSelectedItem(q.categories() == null || q.categories().isEmpty() ? "All" : q.categories().get(0));
        minField.setText(q.minAmount() == null ? "" : q.minAmount().toString());
        maxField.setText(q.maxAmount() == null ? "" : q.maxAmount().toString());
        keywordField.setText(q.keywords() == null ? "" : String.join(", ", q.keywords()));
        showTable(r.records());
        summaryLabel.setText(r.count() + " records, total " + won(r.total()));
        String info = r.records().isEmpty() ? "No results. " : "";
        infoLabel.setText(r.usedRuleParser() ? info + "Simple search was used (LLM not available)." : info.isEmpty() ? " " : info);
    }

    /**
     * Selects the month shown in the Monthly, Statistics and Budget tabs.
     * @param month the month
     */
    public void selectMonth(YearMonth month) {
        currentMonth = month;
        monthLabels.forEach(l -> l.setText(month.toString()));
        showSummary(statisticsService.getMonthlySummary(month));
        showStats(statisticsService.getCategoryStats(month));
        showUsage(budgetManager.getUsage(month));
    }

    /**
     * Shows total income, total expense, balance and records of the month.
     * @param s summary
     */
    public void showSummary(MonthlySummary s) {
        incomeLabel.setText(won(s.income()));
        expenseLabel.setText(won(s.expense()));
        balanceLabel.setText(won(s.balance()));
        monthModel.setRows(s.records());
    }

    /**
     * Shows each category with its amount and share. Shows "No expenses" if the list is empty.
     * @param stats category statistics
     */
    public void showStats(List<CategoryStat> stats) {
        statModel.setRowCount(0);
        long total = 0;
        for (CategoryStat s : stats) {
            statModel.addRow(new Object[]{s.category(), s.amount(), String.format("%.1f", s.percent()), (int) Math.round(s.percent())});
            total += s.amount();
        }
        statTotalLabel.setText(stats.isEmpty() ? "No expenses" : "Total expense: " + won(total));
    }

    /**
     * Checks that the text is a positive number. Shows an error if not.
     * @param text amount text
     * @return true if valid
     */
    public boolean validateAmount(String text) {
        try {
            if (Long.parseLong(text.trim().replace(",", "")) > 0) return true;
        } catch (NumberFormatException ignored) {
        }
        JOptionPane.showMessageDialog(this, "The amount must be a positive number.", "Invalid amount", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    /**
     * Called when Save is clicked in the Budget tab.
     * @param category category name
     * @param amount amount text
     */
    public void clickSaveBudget(String category, String amount) {
        if (!validateAmount(amount)) return;
        budgetManager.setBudget(category, Long.parseLong(amount.trim().replace(",", "")));
        budgetAmountField.setText("");
        showUsage(budgetManager.getUsage(currentMonth));
    }

    /**
     * Shows the budget, the spent amount and the usage of each category.
     * @param usage usage list
     */
    public void showUsage(List<BudgetUsage> usage) {
        usagePanel.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 8);
        c.anchor = GridBagConstraints.WEST;
        int row = 0;
        if (usage.isEmpty()) {
            c.weightx = 1;
            usagePanel.add(new JLabel("No budget is set."), c);
            c.weightx = 0;
        }
        for (BudgetUsage u : usage) {
            c.gridy = row++;
            c.gridx = 0;
            usagePanel.add(new JLabel(u.category()), c);
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue((int) Math.min(100, Math.round(u.percent())));
            bar.setPreferredSize(new Dimension(260, 18));
            if (u.isOver()) bar.setForeground(new Color(0xC62828));
            c.gridx = 1;
            usagePanel.add(bar, c);
            c.gridx = 2;
            usagePanel.add(new JLabel(String.format("%.0f%%   %,d / %,d won", u.percent(), u.spent(), u.budget())), c);
            JLabel over = new JLabel(u.isOver() ? "Over budget" : "");
            over.setForeground(new Color(0xC62828));
            c.gridx = 3;
            usagePanel.add(over, c);
            c.gridx = 4;
            c.weightx = 1;
            usagePanel.add(Box.createHorizontalGlue(), c);
            c.weightx = 0;
        }
        usagePanel.revalidate();
        usagePanel.repaint();
    }

    /**
     * Fills the records table with the given records.
     * @param records records to show
     */
    public void showTable(List<Record> records) { recordModel.setRows(records); }

    /** Table model for a list of records. */
    private static class RecordTableModel extends AbstractTableModel {
        private static final String[] COLS = {"Date", "Type", "Amount", "Category", "Memo"};
        private List<Record> rows = List.of();

        void setRows(List<Record> rows) { this.rows = rows; fireTableDataChanged(); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int c) { return COLS[c]; }
        @Override public Class<?> getColumnClass(int c) { return c == 2 ? Long.class : String.class; }
        @Override public Object getValueAt(int r, int c) {
            Record x = rows.get(r);
            return switch (c) {
                case 0 -> x.getDate().toString();
                case 1 -> x.getType() == Record.Type.INCOME ? "Income" : "Expense";
                case 2 -> x.getAmount();
                case 3 -> x.getCategory();
                default -> x.getMemo();
            };
        }
    }
}
