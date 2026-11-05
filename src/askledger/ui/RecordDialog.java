package askledger.ui;

import askledger.model.Record;
import askledger.service.RecordManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** 기록 추가/수정 입력창 */
public class RecordDialog extends JDialog implements ActionListener {
    private RecordManager recordManager;
    private JTextField dateField = new JTextField(12);
    private JComboBox<String> typeBox = new JComboBox<>(new String[]{"Expense", "Income"});
    private JTextField amountField = new JTextField(12);
    private JComboBox<String> categoryBox = new JComboBox<>();
    private JTextField memoField = new JTextField(20);
    private JButton saveButton = new JButton("Save");
    private JButton cancelButton = new JButton("Cancel");

    private Record editing = null;   // 수정 중인 기록 (추가할 때는 null)
    private Record result = null;   // 저장된 기록 (취소하면 null)

    public RecordDialog(Frame owner, RecordManager recordManager, List<String> categories) {
        super(owner, true);
        this.recordManager = recordManager;
        for (String c : categories) {
            categoryBox.addItem(c);
        }

        JPanel form = new JPanel(new GridLayout(5, 2, 5, 5));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(new JLabel("Date (yyyy-MM-dd)"));
        form.add(dateField);
        form.add(new JLabel("Type"));
        form.add(typeBox);
        form.add(new JLabel("Amount (won)"));
        form.add(amountField);
        form.add(new JLabel("Category"));
        form.add(categoryBox);
        form.add(new JLabel("Memo"));
        form.add(memoField);

        saveButton.addActionListener(this);
        cancelButton.addActionListener(this);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == saveButton) {
            clickSave();
        } else if (e.getSource() == cancelButton) {
            dispose();
        }
    }

    /** 빈 입력창을 연다. 새 기록을 돌려주고 취소하면 null */
    public Record showDialog() {
        setTitle("Add Record");
        dateField.setText(LocalDate.now().toString());
        setVisible(true);
        return result;
    }

    /** 기록 값이 채워진 입력창을 연다. 수정된 기록을 돌려주고 취소하면 null */
    public Record showDialog(Record record) {
        setTitle("Edit Record");
        editing = record;
        dateField.setText(record.getDate().toString());
        if (record.getType() == Record.Type.INCOME) {
            typeBox.setSelectedItem("Income");
        } else {
            typeBox.setSelectedItem("Expense");
        }
        amountField.setText(String.valueOf(record.getAmount()));
        categoryBox.setSelectedItem(record.getCategory());
        memoField.setText(record.getMemo());
        setVisible(true);
        return result;
    }

    /** 저장 버튼: 입력값을 확인하고 기록을 추가하거나 수정한다 */
    public void clickSave() {
        if (!validateInput()) {
            return;
        }
        LocalDate date = LocalDate.parse(dateField.getText().trim());
        Record.Type type = Record.Type.EXPENSE;
        if (typeBox.getSelectedItem().equals("Income")) {
            type = Record.Type.INCOME;
        }
        long amount = Long.parseLong(amountField.getText().trim().replace(",", ""));
        String category = (String) categoryBox.getSelectedItem();
        Record record = new Record(0, date, type, amount, category, memoField.getText().trim());

        boolean saved;
        if (editing == null) {
            saved = recordManager.addRecord(record);
        } else {
            saved = recordManager.updateRecord(editing.getId(), record);
        }
        if (!saved) {
            JOptionPane.showMessageDialog(this, "The data file could not be saved.");
        }
        result = record;
        dispose();
    }

    /** 날짜 형식과 금액(양수)을 확인. 틀리면 오류창 */
    public boolean validateInput() {
        try {
            LocalDate.parse(dateField.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Enter the date as yyyy-MM-dd.");
            return false;
        }
        try {
            long amount = Long.parseLong(amountField.getText().trim().replace(",", ""));
            if (amount > 0) {
                return true;
            }
        } catch (NumberFormatException e) {
        }
        JOptionPane.showMessageDialog(this, "The amount must be a positive number.");
        return false;
    }
}
