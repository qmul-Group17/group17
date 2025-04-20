package view;

import model.Transaction;
import model.Currency;
import service.CurrencyConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;

public class TransactionDialog extends JDialog {

    private JComboBox<Transaction.Type> typeBox;
    private JComboBox<String> categoryBox;
    private JTextField amountField;
    private JComboBox<Currency> currencyBox;
    private JLabel convertedAmountLabel;
    private JTextField dateField;
    private JTextField noteField;
    private JTextField sourceField;

    private Transaction transaction;
    private final String[] expenseCategories = {
            "Food", "Transport", "Shopping", "Health", "Travel", "Beauty", "Entertainment", "Transfer",
            "Housing", "Social", "Education", "Communication", "Red Packet", "Investment",
            "Lend", "Repayment", "Parenting", "Pet", "Other"
    };

    private final String[] incomeCategories = {
            "Transfer In", "Salary", "Investment", "Red Packet In", "Borrow", "Receive", "Other"
    };
    private CurrencyConverter converter;
    private boolean submitted = false;

    public TransactionDialog(Frame owner) {
        super(owner, "Add Transaction", true);
        converter = new CurrencyConverter();
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        typeBox = new JComboBox<>(Transaction.Type.values());
        add(typeBox, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        categoryBox = new JComboBox<>(new String[]{"Food", "Salary", "Transport", "Lend", "Borrow", "Other"});
        add(categoryBox, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Enter Amount:"), gbc);
        gbc.gridx = 1;
        JPanel amountPanel = new JPanel(new GridBagLayout());
        GridBagConstraints apc = new GridBagConstraints();
        apc.insets = new Insets(0, 0, 0, 4);
        apc.fill = GridBagConstraints.HORIZONTAL;
        apc.weightx = 0.7;
        amountField = new JTextField(12);
        amountPanel.add(amountField, apc);

        apc.gridx = 1;
        apc.weightx = 0.3;
        currencyBox = new JComboBox<>(Currency.values());
        currencyBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(value.name()));
        currencyBox.setPreferredSize(new Dimension(80, 24));
        amountPanel.add(currencyBox, apc);
        add(amountPanel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Display Amount (CNY):"), gbc);
        gbc.gridx = 1;
        convertedAmountLabel = new JLabel("0.00");
        add(convertedAmountLabel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        dateField = new JTextField(LocalDate.now().toString());
        add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Note:"), gbc);
        gbc.gridx = 1;
        noteField = new JTextField();
        add(noteField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        add(new JLabel("Source:"), gbc);
        gbc.gridx = 1;
        sourceField = new JTextField("Manual");
        add(sourceField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(this::saveTransaction);
        add(saveButton, gbc);
        gbc.gridx = 1;
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        add(cancelButton, gbc);

        amountField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateConvertedAmount());
        currencyBox.addActionListener(e -> updateConvertedAmount());

        pack();
        setLocationRelativeTo(owner);
    }

    private void updateConvertedAmount() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            Currency selectedCurrency = (Currency) currencyBox.getSelectedItem();
            if (selectedCurrency != null) {
                double cnyAmount = converter.convert(amount, selectedCurrency, Currency.CNY);
                convertedAmountLabel.setText(String.format("%.2f", cnyAmount));
            }
        } catch (NumberFormatException ignored) {
            convertedAmountLabel.setText("0.00");
        }
    }

    private void saveTransaction(ActionEvent e) {
        try {
            double amount = Double.parseDouble(amountField.getText());
            Currency selectedCurrency = (Currency) currencyBox.getSelectedItem();
            double amountInCNY = converter.convert(amount, selectedCurrency, Currency.CNY);

            transaction = new Transaction(
                    (Transaction.Type) typeBox.getSelectedItem(),
                    (String) categoryBox.getSelectedItem(),
                    amountInCNY,
                    LocalDate.parse(dateField.getText()),
                    noteField.getText(),
                    sourceField.getText()
            );
            submitted = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }
}
