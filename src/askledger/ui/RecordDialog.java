package askledger;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Input form for adding or editing a record.
 */
public class RecordDialog extends JDialog {
    private final RecordManager recordManager;
    private final JTextField date = new JTextField(12);
    private final JRadioButton income = new JRadioButton("Income");
    private final JRadioButton expense = new JRadioButton("Expense", true);
    private final JTextField amount = new JTextField(12);
    private final JComboBox<String> category;
    private final JTextField memo = new JTextField(20);
    private Record editing;
    private Record result;

    /**
     * @param owner parent window
     * @param recordManager used to add or update the record
     * @param categories category names for the combo box
     */
    public RecordDialog(Frame owner, RecordManager recordManager, List<String> categories) {
        super(owner, true);
        this.recordManager = recordManager;
        this.category = new JComboBox<>(categories.toArray(new String[0]));
        ButtonGroup g = new ButtonGroup();
        g.add(income);
        g.add(expense);
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        typePanel.add(income);
        typePanel.add(expense);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        Object[][] rows = {{"Date (yyyy-MM-dd)", date}, {"Type", typePanel}, {"Amount (won)", amount},
                {"Category", category}, {"Memo", memo}};
        for (int r = 0; r < rows.length; r++) {
            c.gridy = r;
            c.gridx = 0;
            form.add(new JLabel((String) rows[r][0]), c);
            c.gridx = 1;
            form.add((Component) rows[r][1], c);
        }
        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        save.addActionListener(e -> clickSave());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);
        getRootPane().setDefaultButton(save);
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Opens an empty form.
     * @return the new record, or null if cancelled
     */
    public Record showDialog() {
        setTitle("Add Record");
        date.setText(LocalDate.now().toString());
        setVisible(true);
        return result;
    }

    /**
     * Opens the form filled with the values of the given record.
     * @param record record to edit
     * @return the changed record, or null if cancelled
     */
    public Record showDialog(Record record) {
        setTitle("Edit Record");
        fill(record);
        setVisible(true);
        return result;
    }

    /**
     * Fills the form with a record's values without showing it.
     * @param record record to show
     */
    void fill(Record record) {
        editing = record;
        date.setText(record.getDate().toString());
        (record.getType() == Record.Type.INCOME ? income : expense).setSelected(true);
        amount.setText(String.valueOf(record.getAmount()));
        category.setSelectedItem(record.getCategory());
        memo.setText(record.getMemo());
    }

    /**
     * Called when Save is clicked. Validates the input and adds or updates the record.
     */
    public void clickSave() {
        if (!validateInput()) return;
        Record r = new Record(0, LocalDate.parse(date.getText().trim()),
                income.isSelected() ? Record.Type.INCOME : Record.Type.EXPENSE,
                Long.parseLong(amount.getText().trim().replace(",", "")),
                (String) category.getSelectedItem(), memo.getText().trim());
        boolean ok = editing == null ? recordManager.addRecord(r) : recordManager.updateRecord(editing.getId(), r);
        if (!ok) JOptionPane.showMessageDialog(this, "The data file could not be saved.", "Error", JOptionPane.ERROR_MESSAGE);
        result = r;
        dispose();
    }

    /**
     * Checks that the date is valid and the amount is a positive number. Shows an error if not.
     * @return true if the input is valid
     */
    public boolean validateInput() {
        try {
            LocalDate.parse(date.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Enter the date as yyyy-MM-dd.", "Invalid date", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            if (Long.parseLong(amount.getText().trim().replace(",", "")) > 0) return true;
        } catch (NumberFormatException ignored) {
        }
        JOptionPane.showMessageDialog(this, "The amount must be a positive number.", "Invalid amount", JOptionPane.ERROR_MESSAGE);
        return false;
    }
}
