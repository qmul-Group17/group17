package view;

import model.Transaction;
import model.Currency;
import service.CurrencyConverter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;

public class TransactionDialog extends JDialog {

    // UI components
    private JComboBox<Transaction.Type> typeBox; // Dropdown for transaction type (expense or income)
    private JComboBox<String> categoryBox; // Dropdown for selecting category of the transaction
    private JTextField amountField; // TextField to enter the transaction amount
    private JComboBox<Currency> currencyBox; // Dropdown to select currency
    private JLabel convertedAmountLabel; // Label to show the converted amount in CNY
    private JTextField dateField; // TextField to enter the date of the transaction
    private JTextField noteField; // TextField for entering additional notes
    private JTextField sourceField; // TextField for transaction source (manual, automatic, etc.)

    // Variables for storing transaction information
    private Transaction transaction;

    // Categories for expense and income
    private final String[] expenseCategories = {
            "Food", "Transport", "Shopping", "Health", "Travel", "Beauty", "Entertainment", "Transfer",
            "Housing", "Social", "Education", "Communication", "Red Packet", "Investment",
            "Lend", "Repayment", "Parenting", "Pet", "Other"
    };

    private final String[] incomeCategories = {
            "Transfer In", "Salary", "Investment", "Red Packet In", "Borrow", "Receive", "Other"
    };

    // Currency converter instance for currency conversion logic
    private CurrencyConverter converter;
    private boolean submitted = false; // Flag to check if the transaction was submitted

    // Constructor for the dialog
    public TransactionDialog(Frame owner) {
        super(owner, "Add Transaction", true);
        converter = new CurrencyConverter(); // Initialize the currency converter
        setLayout(new GridBagLayout()); // Use GridBagLayout for UI components
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4); // Add padding to each component
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        // Add "Type" label and dropdown for transaction type selection
        add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        typeBox = new JComboBox<>(Transaction.Type.values());
        add(typeBox, gbc);

        gbc.gridx = 0; gbc.gridy++;
        // Add "Category" label and dropdown for selecting category
        add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        categoryBox = new JComboBox<>(new String[]{"Food", "Salary", "Transport", "Lend", "Borrow", "Other"});
        add(categoryBox, gbc);

        gbc.gridx = 0; gbc.gridy++;
        // Add "Enter Amount" label and input field for entering the transaction amount
        add(new JLabel("Enter Amount:"), gbc);
        gbc.gridx = 1;
        JPanel amountPanel = new JPanel(new GridBagLayout()); // Panel to hold the amount and currency selection
        GridBagConstraints apc = new GridBagConstraints();
        apc.insets = new Insets(0, 0, 0, 4);
        apc.fill = GridBagConstraints.HORIZONTAL;
        apc.weightx = 0.7;
        amountField = new JTextField(12);
        amountPanel.add(amountField, apc);

        // Currency selection dropdown
        apc.gridx = 1;
        apc.weightx = 0.3;
        currencyBox = new JComboBox<>(Currency.values());
        currencyBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> new JLabel(value.name()));
        currencyBox.setPreferredSize(new Dimension(80, 24));
        amountPanel.add(currencyBox, apc);
        add(amountPanel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        // Display the converted amount in CNY
        add(new JLabel("Display Amount (CNY):"), gbc);
        gbc.gridx = 1;
        convertedAmountLabel = new JLabel("0.00");
        add(convertedAmountLabel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        // Add "Date" label and input field for the date
        add(new JLabel("Date (YYYY-MM-DD):"), gbc);
        gbc.gridx = 1;
        dateField = new JTextField(LocalDate.now().toString());
        add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        // Add "Note" label and input field for additional notes
        add(new JLabel("Note:"), gbc);
        gbc.gridx = 1;
        noteField = new JTextField();
        add(noteField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        // Add "Source" label and input field for the source of the transaction
        add(new JLabel("Source:"), gbc);
        gbc.gridx = 1;
        sourceField = new JTextField("Manual");
        add(sourceField, gbc);

        gbc.gridx = 0; gbc.gridy++;
        // Add "Save" button for saving the transaction
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(this::saveTransaction);
        add(saveButton, gbc);

        // Add "Cancel" button for closing the dialog
        gbc.gridx = 1;
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        add(cancelButton, gbc);

        // Update the converted amount when the amount or currency changes
        amountField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateConvertedAmount());
        currencyBox.addActionListener(e -> updateConvertedAmount());

        pack();
        setLocationRelativeTo(owner); // Center the dialog on the screen
    }

    // Update the converted amount based on the selected currency and entered amount
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

    // Save the transaction and convert to CNY before storing
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
            submitted = true; // Mark as submitted
            dispose(); // Close the dialog
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Get the transaction object
    public Transaction getTransaction() {
        return transaction;
    }

    // Check if the transaction has been successfully submitted
    public boolean isSubmitted() {
        return submitted;
    }

    // Interface for handling document events in JTextField
    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }
}
