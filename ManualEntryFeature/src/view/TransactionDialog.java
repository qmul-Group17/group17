package view;

import model.Transaction;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class TransactionDialog extends JDialog {
    private JComboBox<String> typeBox, categoryBox;
    private JTextField amountField, dateField, noteField, sourceField;
    private JButton saveButton;
    private boolean submitted = false;
    private Transaction transaction;

    private final String[] expenseCategories = {
            "Food", "Transport", "Shopping", "Health", "Travel", "Beauty", "Entertainment", "Transfer",
            "Housing", "Social", "Education", "Communication", "Red Packet", "Investment",
            "Lend", "Repayment", "Parenting", "Pet", "Other"
    };

    private final String[] incomeCategories = {
            "Transfer In", "Salary", "Investment", "Red Packet In", "Borrow", "Receive", "Other"
    };

    public TransactionDialog(JFrame parent) {
        super(parent, "Add Transaction", true);
        setLayout(new GridLayout(8, 2, 10, 10));

        add(new JLabel("Type:"));
        typeBox = new JComboBox<>(new String[]{"EXPENSE", "INCOME"});
        add(typeBox);

        add(new JLabel("Category:"));
        categoryBox = new JComboBox<>(expenseCategories);
        add(categoryBox);

        typeBox.addActionListener(e -> {
            categoryBox.removeAllItems();
            String type = (String) typeBox.getSelectedItem();
            String[] categories = type.equals("INCOME") ? incomeCategories : expenseCategories;
            for (String c : categories) {
                categoryBox.addItem(c);
            }
        });

        add(new JLabel("Amount:"));
        amountField = new JTextField();
        add(amountField);

        add(new JLabel("Date (YYYY-MM-DD):"));
        dateField = new JTextField(LocalDate.now().toString());
        add(dateField);

        add(new JLabel("Note:"));
        noteField = new JTextField();
        add(noteField);

        add(new JLabel("Source:"));
        sourceField = new JTextField("Manual");
        add(sourceField);

        saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            try {
                Transaction.Type type = Transaction.Type.valueOf((String) typeBox.getSelectedItem());
                String category = (String) categoryBox.getSelectedItem();
                double amount = Double.parseDouble(amountField.getText());
                LocalDate date = LocalDate.parse(dateField.getText());
                String note = noteField.getText();
                String source = sourceField.getText();

                transaction = new Transaction(type, category, amount, date, note, source);
                submitted = true;
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
            }
        });

        add(new JLabel());
        add(saveButton);

        setSize(400, 350);
        setLocationRelativeTo(parent);
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}