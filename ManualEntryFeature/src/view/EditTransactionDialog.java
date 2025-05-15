package view;

import model.Transaction;
import model.Currency;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class EditTransactionDialog extends JDialog {
    private Transaction modifiedTransaction;
    private JComboBox<Transaction.Type> typeBox;
    private JComboBox<String> categoryBox;
    private JTextField amountField;
    private JComboBox<Currency> currencyBox;
    private JLabel convertedAmountLabel;
    private JTextField dateField;
    private JTextField noteField;
    private JTextField sourceField;
    private boolean submitted = false;

    public EditTransactionDialog(Frame owner, Transaction original) {
        super(owner, "Edit Transaction", true);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Type Selection
        add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        typeBox = new JComboBox<>(Transaction.Type.values());
        typeBox.setSelectedItem(original.getType());
        add(typeBox, gbc);

        // Category Selection (Synchronization type changes)
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        categoryBox = new JComboBox<>();
        updateCategories(original.getType());
        categoryBox.setSelectedItem(original.getCategory());
        add(categoryBox, gbc);

        // Amount Input
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Amount:"), gbc);
        gbc.gridx = 1;
        JPanel amountPanel = new JPanel(new GridBagLayout());
        GridBagConstraints apc = new GridBagConstraints();
        apc.insets = new Insets(0, 0, 0, 4);
        apc.fill = GridBagConstraints.HORIZONTAL;
        apc.weightx = 0.7;
        amountField = new JTextField(String.format("%.2f", original.getAmount()), 12);
        amountPanel.add(amountField, apc);

        // Currency Selection
        apc.gridx = 1;
        apc.weightx = 0.3;
        currencyBox = new JComboBox<>(Currency.values());
        currencyBox.setSelectedItem(Currency.CNY); // Default CNY (same as the add interface)
        amountPanel.add(currencyBox, apc);
        add(amountPanel, gbc);

        // Converted Amount Display
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Converted Amount (CNY):"), gbc);
        gbc.gridx = 1;
        convertedAmountLabel = new JLabel(String.format("%.2f", original.getAmount()));
        add(convertedAmountLabel, gbc);

        // Date Input
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        dateField = new JTextField(original.getDate().toString());
        add(dateField, gbc);

        // Note Input
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Note:"), gbc);
        gbc.gridx = 1;
        noteField = new JTextField(original.getNote());
        add(noteField, gbc);

        // Source Input
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("Source:"), gbc);
        gbc.gridx = 1;
        sourceField = new JTextField(original.getSource());
        add(sourceField, gbc);

        // Buttons
        gbc.gridx = 0;
        gbc.gridy++;
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> saveTransaction(original));
        add(saveButton, gbc);

        gbc.gridx = 1;
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        add(cancelButton, gbc);

        // Synchronization Types and Categories
        typeBox.addActionListener(e -> updateCategories((Transaction.Type) typeBox.getSelectedItem()));

        pack();
        setLocationRelativeTo(owner);
    }

    private void updateCategories(Transaction.Type type) {
        categoryBox.removeAllItems();
        String[] categories = (type == Transaction.Type.INCOME) ?
                new String[]{"Transfer In", "Salary", "Investment", "Red Packet In", "Borrow", "Receive", "Other"} :
                new String[]{"Food", "Transport", "Shopping", "Health", "Travel", "Beauty", "Entertainment",
                        "Housing", "Social", "Education", "Other"};
        for (String category : categories) {
            categoryBox.addItem(category);
        }
    }

    private void saveTransaction(Transaction original) {
        try {
            modifiedTransaction = new Transaction(
                    (Transaction.Type) typeBox.getSelectedItem(),
                    (String) categoryBox.getSelectedItem(),
                    Double.parseDouble(amountField.getText()),
                    LocalDate.parse(dateField.getText()),
                    noteField.getText(),
                    sourceField.getText()
            );
            submitted = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid input: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public Transaction getModifiedTransaction() {
        return modifiedTransaction;
    }

    public boolean isSubmitted() {
        return submitted;
    }
}
