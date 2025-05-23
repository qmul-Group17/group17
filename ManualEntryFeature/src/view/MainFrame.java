package view;

import controller.CSVImporter;
import controller.TransactionController;
import controller.TransactionCategorizer;
import model.Transaction;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//--------------------
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import controller.MLTransactionCategorizer;
import controller.AppConfig;
import view.APISettingsDialog;


public class MainFrame extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private TransactionController controller;
    private JLabel statusLabel; // Add status bar label

    public MainFrame() {// In MainFrame constructor
super("Personal Finance Tracker");

    controller = new TransactionController();
controller.loadTransactions(); // Load data at startup

    setLayout(new BorderLayout());

    // Create menu bar
    JMenuBar menuBar = createMenuBar();
    setJMenuBar(menuBar);

    tableModel = new DefaultTableModel(new String[]{"Type", "Category", "Amount", "Date", "Note", "Source"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // Make the table non-editable
        }
    };
    table = new JTable(tableModel);

// Add table double-click event handler
table.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            // Get selected row and column
            int row = table.getSelectedRow();
            int column = table.getSelectedColumn();

            // Validate row selection
            if (row < 0) return;

            // Handle double-click events
            if (e.getClickCount() == 2) {
                // Category column editing
                if (column == 1) {
                    handleCategoryEdit(row);
                }
                // Full record editing for other columns
                else {
                    handleFullRecordEdit(row);
                }
            }
        }

        /**
         * Handles category-specific editing
         * @param row Selected row index
         */
        private void handleCategoryEdit(int row) {
            // Get current values from table
            String currentType = (String) tableModel.getValueAt(row, 0);
            String currentCategory = (String) tableModel.getValueAt(row, 1);

            // Determine available categories based on transaction type
            String[] categories = getCategoriesForType(currentType);

            // Show category selection dialog
            String newCategory = (String) JOptionPane.showInputDialog(
                    MainFrame.this,
                    "Select transaction category:",
                    "Edit Category",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    categories,
                    currentCategory
            );

            // Update if category changed
            if (newCategory != null && !newCategory.equals(currentCategory)) {
                controller.updateCategory(row, newCategory);
                updateTable();
                statusLabel.setText("Transaction #" + (row+1) + " category updated to: " + newCategory);
            }
        }

        /**
         * Handles full record editing
         * @param row Selected row index
         */
        private void handleFullRecordEdit(int row) {
            // Get original transaction
            Transaction original = controller.getAllTransactions().get(row);

            // Create and show edit dialog
            EditTransactionDialog dialog = new EditTransactionDialog(MainFrame.this, original);
            dialog.setVisible(true);

            // Process changes if submitted
            if (dialog.isSubmitted()) {
                Transaction modified = dialog.getModifiedTransaction();
                controller.updateTransaction(row, modified);
                updateTable();
                statusLabel.setText("Updated transaction #" + (row+1));
            }
        }

        /**
         * Gets appropriate categories based on transaction type
         * @param type Transaction type (INCOME/EXPENSE)
         * @return Array of category strings
         */
        private String[] getCategoriesForType(String type) {
            if ("INCOME".equals(type)) {
                return new String[]{
                        "Transfer In", "Salary", "Investment",
                        "Red Packet In", "Borrow", "Receive", "Other"
                };
            } else {
                return new String[]{
                        "Food", "Transport", "Shopping", "Health",
                        "Travel", "Beauty", "Entertainment", "Transfer",
                        "Housing", "Social", "Education", "Communication",
                        "Red Packet", "Investment", "Lend", "Repayment",
                        "Parenting", "Pet", "Other"
                };
            }
        }
    });



        add(new JScrollPane(table), BorderLayout.CENTER);

        // Create toolbar
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        JButton addBtn = new JButton("Add Transaction");
        addBtn.addActionListener(e -> {
            TransactionDialog dialog = new TransactionDialog(this);
            dialog.setVisible(true);
            if (dialog.isSubmitted()) {
                Transaction t = dialog.getTransaction();
                // Use categorizer to automatically categorize
                Transaction categorizedT = TransactionCategorizer.categorize(t);
                controller.addTransaction(categorizedT);
                updateTable();
                statusLabel.setText("Added new transaction and auto-categorized as: " + categorizedT.getCategory());
            }
        });

        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e -> {
            TableModel model = table.getModel();
            try (FileWriter writer = new FileWriter("output.csv")) {
                // Write column names (title) to CSV file
                for (int i = 0; i < model.getColumnCount(); i++) {
                    writer.append(model.getColumnName(i));
                    if (i < model.getColumnCount() - 1) {
                        writer.append(','); // Column separator, except for last column
                    } else {
                        writer.append('\n'); // Newline after last column
                    }
                }
                // Write data rows to CSV file
                for (int i = 0; i < model.getRowCount(); i++) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        writer.append(String.valueOf(model.getValueAt(i, j))); // Convert value to string and write to CSV
                        if (j < model.getColumnCount() - 1) {
                            writer.append(','); // Column separator, except for last column
                        } else {
                            writer.append('\n'); // Newline after last column, indicating start of new data row
                        }
                    }
                }
                JOptionPane.showMessageDialog(this, "export success");
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(this, "Error writing to file: " + e1.getMessage());
            }
        });

        JButton importBtn = new JButton("Import CSV");
        importBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                List<Transaction> imported = CSVImporter.importFromCSV(filePath);
                // Auto-categorize all transactions before importing
                List<Transaction> categorizedImports = TransactionCategorizer.categorizeAll(imported);
                controller.importTransactions(categorizedImports);
                updateTable();
                statusLabel.setText("Imported and auto-categorized " + imported.size() + " transactions");
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(exportBtn);
        buttonPanel.add(importBtn);
        JButton chartBtn = new JButton("View Chart");//graph
        chartBtn.addActionListener(e -> showChart());
        buttonPanel.add(chartBtn);

        // Add AI categorization button
        JButton recategorizeBtn = new JButton("Recategorize All Transactions");
        recategorizeBtn.addActionListener(e -> {
            controller.recategorizeAll();
            updateTable();
            statusLabel.setText("All transactions recategorized using AI");
        });
        buttonPanel.add(recategorizeBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        // Add status bar
        statusLabel = new JLabel("Ready - Using " +
                (TransactionCategorizer.isUsingAdvancedMode() ? "Advanced Mode" : "Keyword Matching") +
                " for categorization" + (AppConfig.isUseAPI() ? " (API Enabled)" : ""));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.NORTH);

        updateTable(); // Initial load of data to table
        setSize(900, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem importItem = new JMenuItem("Import CSV");
        importItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                List<Transaction> imported = CSVImporter.importFromCSV(filePath);
                // Auto-categorize all transactions before importing
                List<Transaction> categorizedImports = TransactionCategorizer.categorizeAll(imported);
                controller.importTransactions(categorizedImports);
                updateTable();
                statusLabel.setText("Imported and auto-categorized " + imported.size() + " transactions");
            }
        });

        JMenuItem saveItem = new JMenuItem("Save Data");
        saveItem.addActionListener(e -> {
            controller.saveTransactions();
            statusLabel.setText("Data saved");
        });

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            controller.saveTransactions();
            System.exit(0);
        });

        fileMenu.add(importItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem addItem = new JMenuItem("Add Transaction");
        addItem.addActionListener(e -> {
            TransactionDialog dialog = new TransactionDialog(this);
            dialog.setVisible(true);
            if (dialog.isSubmitted()) {
                Transaction t = dialog.getTransaction();
                // Use categorizer to automatically categorize
                Transaction categorizedT = TransactionCategorizer.categorize(t);
                controller.addTransaction(categorizedT);
                updateTable();
                statusLabel.setText("Added new transaction and auto-categorized as: " + categorizedT.getCategory());
            }
        });

        editMenu.add(addItem);

        JMenuItem deleteItem = new JMenuItem("Delete Transaction");
        deleteItem.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                controller.deleteTransaction(selectedRow);
                updateTable();
                statusLabel.setText("Deleted transaction #" + (selectedRow+1));
            } else {
                JOptionPane.showMessageDialog(this, "Please select a transaction to delete");
            }
        });
        editMenu.add(deleteItem);

        JMenu aiMenu = new JMenu("AI Features");
        JMenuItem toggleAIItem = new JMenuItem("Toggle Categorization Mode");
        toggleAIItem.addActionListener(e -> {
            TransactionCategorizer.toggleMode();
            boolean isAdvancedMode = TransactionCategorizer.isUsingAdvancedMode(); // Modified method name
            statusLabel.setText("Current categorization mode: " + (isAdvancedMode ? "Advanced Mode" : "Keyword Matching"));
        });

        JMenuItem recategorizeItem = new JMenuItem("Recategorize All Transactions");
        recategorizeItem.addActionListener(e -> {
            controller.recategorizeAll();
            updateTable();
            statusLabel.setText("All transactions recategorized");
        });

// Add API settings option
        JMenuItem apiSettingsItem = new JMenuItem("AI Categorization API Settings");
        apiSettingsItem.addActionListener(e -> {
            APISettingsDialog dialog = new APISettingsDialog(this);
            dialog.setVisible(true);
        });

        aiMenu.add(toggleAIItem);
        aiMenu.add(recategorizeItem);
        aiMenu.addSeparator(); // Separator
        aiMenu.add(apiSettingsItem);


        // Chart menu
        JMenu chartMenu = new JMenu("Charts");
        JMenuItem viewChartItem = new JMenuItem("View Charts");
        viewChartItem.addActionListener(e -> showChart());
        chartMenu.add(viewChartItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Personal Finance Management System v1.0\n" +
                            "Featuring AI auto-categorization\n\n" +
                            "Tip: Double-click on a category cell to manually modify the category",
                    "About",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(aiMenu);
        menuBar.add(chartMenu);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton addButton = new JButton("Add");
        addButton.addActionListener(e -> {
            TransactionDialog dialog = new TransactionDialog(this);
            dialog.setVisible(true);
            if (dialog.isSubmitted()) {
                Transaction t = dialog.getTransaction();
                // Use categorizer to automatically categorize
                Transaction categorizedT = TransactionCategorizer.categorize(t);
                controller.addTransaction(categorizedT);
                updateTable();
            }
        });

        JButton importButton = new JButton("Import");
        importButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                List<Transaction> imported = CSVImporter.importFromCSV(filePath);
                List<Transaction> categorizedImports = TransactionCategorizer.categorizeAll(imported);
                controller.importTransactions(categorizedImports);
                updateTable();
            }
        });

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                controller.deleteTransaction(selectedRow);
                updateTable();
                statusLabel.setText("Deleted transaction #" + (selectedRow+1));
            } else {
                JOptionPane.showMessageDialog(this, "Please select a transaction to delete");
            }
        });
        toolBar.add(deleteButton);

        JButton editButton = new JButton("Edit");
        editButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                Transaction original = controller.getAllTransactions().get(selectedRow);
                EditTransactionDialog dialog = new EditTransactionDialog(MainFrame.this, original);
                dialog.setVisible(true);
                if (dialog.isSubmitted()) {
                    Transaction modified = dialog.getModifiedTransaction();
                    controller.updateTransaction(selectedRow, modified);
                    updateTable();
                    statusLabel.setText("Updated transaction #" + (selectedRow+1));
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a transaction to edit");
            }
        });
        toolBar.add(editButton);

        JButton chartButton = new JButton("Charts");
        chartButton.addActionListener(e -> showChart());

        JButton toggleModeButton = new JButton("Toggle AI Mode");
        toggleModeButton.addActionListener(e -> {
            TransactionCategorizer.toggleMode();
            boolean isAdvancedMode = TransactionCategorizer.isUsingAdvancedMode();
            statusLabel.setText("Current categorization mode: " +
                    (isAdvancedMode ? "Advanced Mode" : "Keyword Matching") +
                    (AppConfig.isUseAPI() ? " (API Enabled)" : ""));
        });
        // Add transaction button handler
        addButton.addActionListener(e -> {
            TransactionDialog dialog = new TransactionDialog(this);
            dialog.setVisible(true);
            if (dialog.isSubmitted()) {
                Transaction t = dialog.getTransaction();
                // Use controller's categorization method, not calling the categorizer directly
                controller.addAndCategorizeTransaction(t);
                updateTable();
                statusLabel.setText("Added new transaction and auto-categorized as: " + t.getCategory());
            }
        });

// Import CSV button handler
        importButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                // Use controller's import method, which will handle categorization
                controller.importFromCSV(filePath);
                updateTable();
                statusLabel.setText("Imported and auto-categorized transactions");
            }
        });

        toolBar.add(addButton);
        toolBar.add(importButton);
        toolBar.add(chartButton);
        toolBar.addSeparator();
        toolBar.add(toggleModeButton);

        return toolBar;
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        List<Transaction> transactions = controller.getAllTransactions();
        for (Transaction t : transactions) {
            tableModel.addRow(new Object[]{
                    t.getType(), t.getCategory(), t.getAmount(), t.getDate(), t.getNote(), t.getSource()
            });
        }
    }

    private void showChart() {
        List<Transaction> transactions = controller.getAllTransactions();

        // ------- Pie Chart Data -------
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getType() == Transaction.Type.EXPENSE) {
                categoryTotals.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }

        DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            pieDataset.setValue(entry.getKey(), entry.getValue());
        }

        JFreeChart pieChart = ChartFactory.createPieChart(

                "Expense by Category",
                pieDataset,
                true, true, false

        );
        pieChart.setBackgroundPaint(Color.WHITE);
        pieChart.getPlot().setBackgroundPaint(Color.WHITE);
        pieChart.getPlot().setOutlinePaint(null);
        ChartPanel piePanel = new ChartPanel(pieChart);

        // ------- Line Chart Data: Show ALL expense days -------
        TimeSeries series = new TimeSeries("All Expense Dates");

        Map<LocalDate, Double> dailyTotal = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getType() == Transaction.Type.EXPENSE) {
                LocalDate date = t.getDate();
                dailyTotal.merge(date, t.getAmount(), Double::sum);
            }
        }

        List<LocalDate> sortedDates = new ArrayList<>(dailyTotal.keySet());
        sortedDates.sort(Comparator.naturalOrder());

        for (LocalDate date : sortedDates) {
            double amount = dailyTotal.get(date);
            Day day = new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
            series.add(day, amount);
        }

        TimeSeriesCollection lineDataset = new TimeSeriesCollection();
        lineDataset.addSeries(series);

        JFreeChart lineChart = ChartFactory.createTimeSeriesChart(
                "Expense Trend",
                "Date",
                "Amount",
                lineDataset,
                true, true, false
        );
        lineChart.setBackgroundPaint(Color.WHITE); // White background for entire chart
        lineChart.getPlot().setBackgroundPaint(Color.WHITE); // White background for line area
        lineChart.getPlot().setOutlinePaint(null); // Remove border line (optional)
        ChartPanel linePanel = new ChartPanel(lineChart);

        // ------- Create Combined Chart Window -------
        JFrame chartFrame = new JFrame("Expense Charts");

        chartFrame.setLayout(new GridLayout(1, 2)); // Side-by-side
        chartFrame.add(piePanel);
        chartFrame.add(linePanel);

        chartFrame.setSize(1000, 500);
        chartFrame.setLocationRelativeTo(this);
        chartFrame.setVisible(true);
    }
}
