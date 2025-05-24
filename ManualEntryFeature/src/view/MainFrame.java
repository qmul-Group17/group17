package view;

import controller.CSVImporter;
import controller.TransactionController;
import controller.TransactionCategorizer;
import model.Transaction;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.List;
//--------------------
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.axis.DateAxis;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;

import controller.MLTransactionCategorizer;
import controller.AppConfig;
import view.APISettingsDialog;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;

import javax.swing.border.EmptyBorder;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.AbstractBorder;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class MainFrame extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private TransactionController controller;
    private JLabel statusLabel; // Add status bar label
    private User currentUser;
    private JLabel daysLabel;
    private JLabel tipLabel;

    private static final Color PRIMARY_COLOR = new Color(79, 70, 229);
    private static final Color SECONDARY_COLOR = new Color(124, 58, 237);
    private static final Color BACKGROUND_COLOR = new Color(250, 251, 255);
    private static final Color CARD_COLOR = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(31, 41, 55);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color SUCCESS_COLOR = new Color(16, 185, 129);
    private static final Color ERROR_COLOR = new Color(239, 68, 68);

    public MainFrame(User user) {// In MainFrame constructor
        super("Personal Finance Tracker - Chinese Context Enhanced");
        this.currentUser = user;

        controller = new TransactionController(currentUser);
        controller.loadTransactions();// Load data at startup

        setLayout(new BorderLayout());

        // Create menu bar
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        JPanel statsPanel = createStatsPanel();
        add(statsPanel, BorderLayout.EAST);
        updateStatsPanel();

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
             * Handles category-specific editing with enhanced Chinese categories
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
                    statusLabel.setText("Transaction #" + (row + 1) + " category updated to: " + newCategory);
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
                    statusLabel.setText("Updated transaction #" + (row + 1));
                }
            }

            /**
             * Gets appropriate categories based on transaction type with enhanced Chinese context
             * Including Chinese lifestyle categories like Red Packet, Live Streaming Shopping, etc.
             * @param type Transaction type (INCOME/EXPENSE)
             * @return Array of category strings
             */
            private String[] getCategoriesForType(String type) {
                if ("INCOME".equals(type)) {
                    return new String[]{
                            "Transfer In", "Salary", "Investment", "Red Packet",
                            "Freelance", "Side Hustle", "Cash Gift", "Refund",
                            "Borrowing", "Other"
                    };
                } else {
                    return new String[]{
                            "Food", "Transport", "Shopping", "Health", "Travel",
                            "Beauty", "Entertainment", "Transfer", "Housing",
                            "Social", "Education", "Communication", "Red Packet",
                            "Investment", "Lending", "Repayment", "Parenting",
                            "Pet", "Live Stream Shopping", "Group Buying",
                            "Festival Shopping", "Other"
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
                updateStatsPanel();
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
                JOptionPane.showMessageDialog(this, "Export successful");
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
                updateStatsPanel();
                statusLabel.setText("Imported and auto-categorized " + imported.size() + " transactions");
            }
        });

        // FIXED: Define buttonPanel and define aiAnalysisButton before using it
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // FIXED: Define aiAnalysisButton before referencing it
        JButton aiAnalysisButton = new JButton("AI Analysis");

        // FIXED: Removed duplicate buttonsPanel and properly organized buttons
        buttonPanel.add(addBtn);
        buttonPanel.add(exportBtn);
        buttonPanel.add(importBtn);

        JButton chartBtn = new JButton("View Chart");//graph
        chartBtn.addActionListener(e -> showChart());
        buttonPanel.add(chartBtn);

        // Add AI categorization button
        JButton recategorizeBtn = new JButton("Smart Recategorize All");
        recategorizeBtn.addActionListener(e -> {
            controller.recategorizeAll();
            updateTable();

            // 根据当前的分类模式显示不同的状态消息
            if (TransactionCategorizer.isUsingSimpleMode()) {
                statusLabel.setText("All transactions recategorized using keyword matching (including Chinese context)");
            } else {
                // 检查是否启用了API
                if (AppConfig.isUseAPI()) {
                    statusLabel.setText("All transactions recategorized using AI classification with Chinese lifestyle patterns");
                } else {
                    statusLabel.setText("All transactions recategorized using advanced pattern matching");
                }
            }
        });
        buttonPanel.add(recategorizeBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add status bar
        statusLabel = new JLabel("Ready - Using " +
                (TransactionCategorizer.isUsingAdvancedMode() ? "Advanced Mode" : "Keyword Matching") +
                " for categorization" + (AppConfig.isUseAPI() ? " (API Enabled)" : "") +
                " - Spring Festival Red Packet Detection Active");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.NORTH);

        updateTable(); // Initial load of data to table
        setSize(1000, 450);
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
                updateStatsPanel();
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
                updateStatsPanel();
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
                statusLabel.setText("Deleted transaction #" + (selectedRow + 1));
            } else {
                JOptionPane.showMessageDialog(this, "Please select a transaction to delete");
            }
        });
        editMenu.add(deleteItem);

        JMenu aiMenu = new JMenu("AI Features");
        JMenuItem toggleAIItem = new JMenuItem("Toggle Categorization Mode");
        toggleAIItem.addActionListener(e -> {
            TransactionCategorizer.toggleMode();
            boolean isAdvancedMode = TransactionCategorizer.isUsingAdvancedMode();
            statusLabel.setText("Current categorization mode: " + (isAdvancedMode ? "Advanced Mode (Chinese Context Aware)" : "Keyword Matching"));
        });

        JMenuItem recategorizeItem = new JMenuItem("Smart Recategorize All Transactions");
        recategorizeItem.addActionListener(e -> {
            controller.recategorizeAll();
            updateTable();
            statusLabel.setText("All transactions recategorized with Chinese lifestyle awareness");
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
        JMenu chartMenu = new JMenu("Charts & Analysis");
        JMenuItem viewChartItem = new JMenuItem("View Charts & Smart Analysis");
        viewChartItem.addActionListener(e -> showChart());
        chartMenu.add(viewChartItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Personal Finance Management System v2.0\n" +
                            "Enhanced with Chinese Context AI Classification\n\n" +
                            "Features:\n" +
                            "• Smart Spring Festival Red Packet Detection\n" +
                            "• Chinese Shopping Festival Recognition (Double 11, 618)\n" +
                            "• Live Streaming & Group Buying Detection\n" +
                            "• AI-Powered Financial Analysis\n\n" +
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
                updateStatsPanel();
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
                updateStatsPanel();
            }
        });

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                controller.deleteTransaction(selectedRow);
                updateTable();
                statusLabel.setText("Deleted transaction #" + (selectedRow + 1));
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
                    statusLabel.setText("Updated transaction #" + (selectedRow + 1));
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a transaction to edit");
            }
        });
        toolBar.add(editButton);

        JButton chartButton = new JButton("Charts & Analysis");
        chartButton.addActionListener(e -> showChart());

        JButton toggleModeButton = new JButton("Toggle Smart Mode");
        toggleModeButton.addActionListener(e -> {
            TransactionCategorizer.toggleMode();
            boolean isAdvancedMode = TransactionCategorizer.isUsingAdvancedMode();
            statusLabel.setText("Current categorization mode: " +
                    (isAdvancedMode ? "Advanced Mode (Chinese Context)" : "Keyword Matching") +
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
                updateStatsPanel();
                statusLabel.setText("Imported and auto-categorized transactions with Chinese context awareness");
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

    /**
     * Convert report text, replacing bold markers (asterisks) with uppercase text
     *
     * @param report Original report text
     * @return Processed report text
     */
    private String formatReportText(String report) {
        if (report == null || report.isEmpty()) {
            return report;
        }

        StringBuilder result = new StringBuilder();
        boolean inBoldSection = false;
        StringBuilder boldText = new StringBuilder();

        for (int i = 0; i < report.length(); i++) {
            char currentChar = report.charAt(i);

            // Check if it's a bold marker (two consecutive asterisks)
            if (currentChar == '*' && i + 1 < report.length() && report.charAt(i + 1) == '*') {
                // Toggle bold state
                inBoldSection = !inBoldSection;

                // Skip two asterisks
                i++;

                // If it's the end of bold section, process the collected text
                if (!inBoldSection) {
                    // Convert bold text to uppercase and add to result
                    result.append(boldText.toString().toUpperCase());
                    boldText = new StringBuilder(); // Reset bold text collector
                }
            } else if (inBoldSection) {
                // If inside bold section, collect text instead of directly adding
                boldText.append(currentChar);
            } else {
                // Normal text, directly add to result
                result.append(currentChar);
            }
        }

        // Handle possible unclosed bold markers
        if (inBoldSection) {
            result.append("**").append(boldText.toString());
        }

        return result.toString();
    }

    /**
     * Generate financial analysis report using DeepSeek API with enhanced Chinese context
     *
     * @param transactions  List of transactions
     * @param selectedMonth Selected month
     * @return Generated analysis report
     */
    private String generateFinancialAnalysisViaAPI(List<Transaction> transactions, YearMonth selectedMonth) throws Exception {
        // Check if API is enabled
        if (!AppConfig.isUseAPI()) {
            return generateLocalFinancialAnalysis(transactions, selectedMonth);
        }

        // Get API configuration
        String apiUrl = AppConfig.getApiUrl();
        String apiKey = AppConfig.getApiKey();

        if (apiUrl.isEmpty() || apiKey.isEmpty()) {
            throw new Exception("API URL or Key is not configured. Please set them in the API Settings.");
        }

        try {
            // If no month selected, use the most recent month
            YearMonth targetMonth = selectedMonth;
            if (targetMonth == null) {
                // Find the most recent month
                LocalDate mostRecent = LocalDate.MIN;
                for (Transaction t : transactions) {
                    if (t.getDate().isAfter(mostRecent)) {
                        mostRecent = t.getDate();
                    }
                }
                targetMonth = YearMonth.from(mostRecent);
            }

            // Filter transactions for the target month
            List<Transaction> monthlyTransactions = new ArrayList<>();
            for (Transaction t : transactions) {
                if (YearMonth.from(t.getDate()).equals(targetMonth)) {
                    monthlyTransactions.add(t);
                }
            }

            // Prepare data summary for API analysis
            double totalIncome = 0;
            double totalExpense = 0;
            Map<String, Double> expenseByCategory = new HashMap<>();
            Map<String, Double> incomeByCategory = new HashMap<>();

            for (Transaction t : monthlyTransactions) {
                if (t.getType() == Transaction.Type.INCOME) {
                    totalIncome += t.getAmount();
                    incomeByCategory.merge(t.getCategory(), t.getAmount(), Double::sum);
                } else {
                    totalExpense += t.getAmount();
                    expenseByCategory.merge(t.getCategory(), t.getAmount(), Double::sum);
                }
            }

            // ===== NEW: BUDGET HABIT ANALYSIS DATA =====
            double currentBudget = getCurrentMonthlyBudget(); // Get from UI
            double budgetUtilization = currentBudget > 0 ? (totalExpense / currentBudget * 100) : 0;
            double budgetRemaining = currentBudget - totalExpense;

            // Analyze historical budget performance (last 3 months)
            Map<YearMonth, Double> historicalBudgetPerformance = getHistoricalBudgetPerformance(transactions, targetMonth, 3);

            // Analyze spending patterns vs budget by category
            Map<String, Double> categoryBudgetAnalysis = analyzeCategoryBudgetHabits(expenseByCategory, currentBudget);

            // Build API request with Chinese context awareness
            StringBuilder prompt = new StringBuilder();
            prompt.append("As a financial analyst specializing in Chinese consumer behavior, review the following monthly transaction data and create a comprehensive financial analysis report with actionable recommendations. ");
            prompt.append("IMPORTANT: Include detailed BUDGET HABIT ANALYSIS as a key section in your report. ");
            prompt.append("Consider Chinese lifestyle patterns including shopping festivals (Double 11, 618), red packet culture, live streaming purchases, group buying trends, and seasonal spending habits. ");
            prompt.append("The report should include monthly summary, BUDGET HABIT ANALYSIS, expense analysis with Chinese context, income sources analysis, unusual expenses, personalized financial recommendations, and an action plan for next month.\n\n");

            // Add month information with seasonal context
            prompt.append("Month: ").append(targetMonth.getMonth()).append(" ").append(targetMonth.getYear());

            // Add seasonal context analysis
            String seasonalContext = getSeasonalContext(targetMonth);
            if (!seasonalContext.isEmpty()) {
                prompt.append(" (").append(seasonalContext).append(")");
            }
            prompt.append("\n\n");

            // Add overall financial summary
            prompt.append("FINANCIAL SUMMARY:\n");
            prompt.append("- Total Income: ¥").append(String.format("%.2f", totalIncome)).append("\n");
            prompt.append("- Total Expenses: ¥").append(String.format("%.2f", totalExpense)).append("\n");
            prompt.append("- Net Savings: ¥").append(String.format("%.2f", totalIncome - totalExpense)).append("\n");
            prompt.append("- Savings Rate: ").append(String.format("%.1f%%", totalIncome > 0 ? (totalIncome - totalExpense) / totalIncome * 100 : 0)).append("\n\n");

            // ===== NEW: BUDGET HABIT ANALYSIS SECTION =====
            prompt.append("BUDGET HABIT ANALYSIS DATA:\n");
            prompt.append("- Monthly Budget Target: ¥").append(String.format("%.2f", currentBudget)).append("\n");
            prompt.append("- Budget Utilization: ").append(String.format("%.1f%%", budgetUtilization)).append(" (¥").append(String.format("%.2f", totalExpense)).append(" spent)\n");
            prompt.append("- Budget Remaining/Overspend: ¥").append(String.format("%.2f", budgetRemaining));
            if (budgetRemaining >= 0) {
                prompt.append(" (remaining)\n");
            } else {
                prompt.append(" (overspend)\n");
            }

            prompt.append("- Budget Status Classification: ");
            if (budgetUtilization <= 80) {
                prompt.append("UNDER BUDGET - Good control\n");
            } else if (budgetUtilization <= 100) {
                prompt.append("ON BUDGET - Monitor closely\n");
            } else {
                prompt.append("OVER BUDGET - Immediate attention needed\n");
            }

            // Historical budget performance
            prompt.append("\nHistorical Budget Performance:\n");
            for (Map.Entry<YearMonth, Double> entry : historicalBudgetPerformance.entrySet()) {
                prompt.append("- ").append(entry.getKey().getMonth().name().substring(0, 3)).append(" ")
                        .append(entry.getKey().getYear()).append(": ").append(String.format("%.1f%%", entry.getValue()))
                        .append(" utilization\n");
            }

            // Category budget breakdown
            prompt.append("\nCategory Budget Allocation:\n");
            for (Map.Entry<String, Double> entry : categoryBudgetAnalysis.entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(String.format("%.1f%%", entry.getValue()))
                        .append(" of total budget\n");
            }
            prompt.append("\n");

            // Add expense categories breakdown with Chinese context
            prompt.append("EXPENSE CATEGORIES (with Chinese context analysis):\n");
            List<Map.Entry<String, Double>> sortedExpenses = new ArrayList<>(expenseByCategory.entrySet());
            sortedExpenses.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            for (Map.Entry<String, Double> entry : sortedExpenses) {
                double percentage = totalExpense > 0 ? (entry.getValue() / totalExpense) * 100 : 0;
                prompt.append("- ").append(entry.getKey()).append(": ¥").append(String.format("%.2f", entry.getValue()))
                        .append(" (").append(String.format("%.1f%%", percentage)).append(")");

                // Add Chinese context for specific categories
                String contextNote = getCategoryContext(entry.getKey(), targetMonth);
                if (!contextNote.isEmpty()) {
                    prompt.append(" [").append(contextNote).append("]");
                }
                prompt.append("\n");
            }
            prompt.append("\n");

            // Add income sources breakdown
            prompt.append("INCOME SOURCES:\n");
            List<Map.Entry<String, Double>> sortedIncome = new ArrayList<>(incomeByCategory.entrySet());
            sortedIncome.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            for (Map.Entry<String, Double> entry : sortedIncome) {
                double percentage = totalIncome > 0 ? (entry.getValue() / totalIncome) * 100 : 0;
                prompt.append("- ").append(entry.getKey()).append(": ¥").append(String.format("%.2f", entry.getValue()))
                        .append(" (").append(String.format("%.1f%%", percentage)).append(")\n");
            }
            prompt.append("\n");

            // Add top 5 largest expenses with context analysis
            prompt.append("TOP 5 LARGEST EXPENSES (with behavioral analysis):\n");
            List<Transaction> sortedByAmount = new ArrayList<>(monthlyTransactions);
            sortedByAmount.removeIf(t -> t.getType() == Transaction.Type.INCOME);
            sortedByAmount.sort((t1, t2) -> Double.compare(t2.getAmount(), t1.getAmount()));

            for (int i = 0; i < Math.min(5, sortedByAmount.size()); i++) {
                Transaction t = sortedByAmount.get(i);
                prompt.append("- ").append(t.getCategory()).append(": ¥").append(String.format("%.2f", t.getAmount()))
                        .append(" on ").append(t.getDate()).append(" (").append(t.getNote()).append(")");

                // Add behavioral context
                String behaviorContext = getTransactionBehaviorContext(t);
                if (!behaviorContext.isEmpty()) {
                    prompt.append(" [").append(behaviorContext).append("]");
                }
                prompt.append("\n");
            }
            prompt.append("\n");

            // Enhanced format requirements with Chinese context
            prompt.append("FORMAT REQUIREMENTS:\n");
            prompt.append("1. Create a friendly, culturally-aware financial report with practical advice for Chinese consumers\n");
            prompt.append("2. Use normal sentence case - NO ALL CAPS text anywhere in the report\n");
            prompt.append("3. MUST include a dedicated 'Budget Habit Analysis' section with specific budget recommendations\n");
            prompt.append("4. Consider Chinese spending patterns like festival shopping, group buying, live streaming purchases\n");
            prompt.append("5. Reference relevant Chinese consumer behaviors and cultural spending habits\n");
            prompt.append("6. Use short paragraphs and bullet points for readability\n");
            prompt.append("7. Include practical recommendations considering Chinese financial products and services\n");
            prompt.append("8. For any recommendation, explain briefly WHY it would be helpful in Chinese context\n");
            prompt.append("9. DO NOT use any markdown formatting like asterisks (**) for bold text\n");

            // ===== NEW: BUDGET HABIT SPECIFIC REQUIREMENTS =====
            prompt.append("\nBUDGET HABIT ANALYSIS REQUIREMENTS:\n");
            prompt.append("1. Analyze current month's budget performance vs target and historical trends\n");
            prompt.append("2. Identify budget management strengths and areas for improvement\n");
            prompt.append("3. Provide specific actionable budget recommendations for Chinese consumers\n");
            prompt.append("4. Address seasonal budgeting for shopping festivals (Double 11, 618) and Chinese holidays\n");
            prompt.append("5. Suggest Chinese budget tracking methods and apps (like 口袋记账, Timi Time)\n");
            prompt.append("6. Recommend budget allocation strategies considering Chinese lifestyle patterns\n");
            prompt.append("7. Include cultural spending considerations (red packets, family obligations, gift-giving)\n");

            prompt.append("\nCHINESE CONTEXT REQUIREMENTS:\n");
            prompt.append("1. Consider shopping festival seasons (Double 11, 618, Chinese New Year) and their impact on spending\n");
            prompt.append("2. Analyze red packet patterns during Spring Festival period\n");
            prompt.append("3. Identify live streaming shopping and group buying trends\n");
            prompt.append("4. Consider typical Chinese savings habits and investment preferences\n");
            prompt.append("5. Reference popular Chinese payment methods and financial apps\n");
            prompt.append("6. Acknowledge cultural spending on family, gifts, and social obligations\n");
            prompt.append("7. Consider seasonal food and travel patterns specific to China\n");

            prompt.append("\nCONTENT REQUIREMENTS:\n");
            prompt.append("1. Keep the report concise but culturally relevant\n");
            prompt.append("2. Focus 25% on budget habit analysis, 45% on practical recommendations and 30% on other analysis\n");
            prompt.append("3. Highlight unusual patterns with cultural context\n");
            prompt.append("4. Use relatable examples from Chinese consumer behavior\n");
            prompt.append("5. Include actionable steps considering Chinese financial ecosystem\n");
            prompt.append("6. Mention relevant Chinese financial trends and opportunities\n");
            prompt.append("7. Congratulate positive financial behaviors common in Chinese culture\n");

            // ===== NEW: BUDGET HABIT EXAMPLES =====
            prompt.append("\nEXAMPLE BUDGET HABIT ANALYSIS CONTENT:\n");
            prompt.append("If budget utilization is 85%: 'Your budget utilization of 85% shows good spending discipline. Consider allocating the remaining 15% to an emergency fund or Yu'e Bao for better returns.'\n");
            prompt.append("If over budget during shopping festival: 'Budget exceeded by 20% during Double 11 - this is common but consider setting up a separate festival shopping budget next year to protect your regular monthly budget.'\n");
            prompt.append("If consistently under budget: 'Three months of under-budget performance suggests you could either increase your savings goals or allow more flexibility for quality of life improvements like dining out or entertainment.'\n");

            prompt.append("\nEXAMPLE CHINESE CONTEXT ANALYSIS:\n");
            prompt.append("If shopping expenses are high in November: 'Your November shopping surge aligns with Double 11 festival - this is typical behavior but consider setting a pre-shopping budget next year.'\n");
            prompt.append("If red packet income appears: 'Red packet income indicates Spring Festival period - consider saving a portion for future gift-giving reciprocity.'\n");
            prompt.append("If live streaming purchases detected: 'Live streaming purchases suggest impulse buying - consider implementing a 24-hour waiting period for non-essential items.'\n");

            // Build API request body
            String requestBody = "{"
                    + "\"model\": \"deepseek-chat\","
                    + "\"messages\": ["
                    + "  {\"role\": \"user\", \"content\": \"" + escapeJson(prompt.toString()) + "\"}"
                    + "],"
                    + "\"temperature\": 0.2,"
                    + "\"max_tokens\": 3000"  // Increased for budget analysis
                    + "}";

            // Send API request
            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read response
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Parse API response to extract content
                    return parseDeepSeekResponse(response.toString());
                }
            } else {
                throw new Exception("API call failed with HTTP error code: " + responseCode);
            }
        } catch (Exception e) {
            throw new Exception("Failed to generate analysis via API: " + e.getMessage(), e);
        }
    }

// ===== NEW HELPER METHODS FOR BUDGET ANALYSIS =====

    /**
     * Get current monthly budget from UI
     * This should integrate with your chart UI budget value
     */
    private double getCurrentMonthlyBudget() {
        // You can modify this to get the actual budget from your UI
        // For now, using a default value that should be replaced with actual UI value
        return 2000.0; // Default budget - replace with actual UI budget value
    }

    /**
     * Analyze historical budget performance for the last few months
     */
    private Map<YearMonth, Double> getHistoricalBudgetPerformance(List<Transaction> transactions, YearMonth currentMonth, int monthsBack) {
        Map<YearMonth, Double> performance = new LinkedHashMap<>();
        double currentBudget = getCurrentMonthlyBudget();

        for (int i = 1; i <= monthsBack; i++) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            double monthlyExpense = transactions.stream()
                    .filter(t -> t.getType() == Transaction.Type.EXPENSE)
                    .filter(t -> YearMonth.from(t.getDate()).equals(targetMonth))
                    .mapToDouble(Transaction::getAmount)
                    .sum();

            double budgetUtilization = currentBudget > 0 ? (monthlyExpense / currentBudget) * 100 : 0;
            performance.put(targetMonth, budgetUtilization);
        }

        return performance;
    }

    /**
     * Analyze category spending as percentage of total budget
     */
    private Map<String, Double> analyzeCategoryBudgetHabits(Map<String, Double> expenseByCategory, double totalBudget) {
        Map<String, Double> categoryBudgetPercentage = new HashMap<>();

        if (totalBudget > 0) {
            for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
                double percentage = (entry.getValue() / totalBudget) * 100;
                categoryBudgetPercentage.put(entry.getKey(), percentage);
            }
        }

        return categoryBudgetPercentage;
    }

    /**
     * Get seasonal context for Chinese consumers
     */
    private String getSeasonalContext(YearMonth month) {
        switch (month.getMonth()) {
            case JANUARY:
            case FEBRUARY:
                return "Spring Festival period - expect red packets and family spending";
            case JUNE:
                return "618 Shopping Festival season";
            case NOVEMBER:
                return "Double 11 Shopping Festival peak";
            case DECEMBER:
                return "Year-end bonuses and holiday shopping";
            default:
                return "";
        }
    }

    /**
     * Get category context for Chinese spending patterns
     */
    private String getCategoryContext(String category, YearMonth month) {
        switch (category.toLowerCase()) {
            case "shopping":
                if (month.getMonth().getValue() == 11) {
                    return "Double 11 shopping festival impact";
                } else if (month.getMonth().getValue() == 6) {
                    return "618 shopping festival impact";
                }
                return "Consider if live streaming or group buying influenced purchases";
            case "red packet":
                return "Traditional Spring Festival gift exchange";
            case "food":
                if (month.getMonth().getValue() == 1 || month.getMonth().getValue() == 2) {
                    return "Spring Festival reunion meals and treats";
                }
                return "Consider food delivery and dining out patterns";
            case "transport":
                if (month.getMonth().getValue() == 1 || month.getMonth().getValue() == 2) {
                    return "Spring Festival travel rush";
                }
                return "Ride-hailing and public transport usage";
            case "entertainment":
                return "KTV, movies, and digital entertainment subscriptions";
            default:
                return "";
        }
    }

    /**
     * Get behavioral context for individual transactions
     */
    private String getTransactionBehaviorContext(Transaction t) {
        String note = t.getNote().toLowerCase();
        String source = t.getSource().toLowerCase();
        String combined = note + " " + source;

        if (combined.contains("live") || combined.contains("stream") || combined.contains("直播")) {
            return "Live streaming purchase - consider impulse buying control";
        }
        if (combined.contains("group") || combined.contains("团购") || combined.contains("拼")) {
            return "Group buying - good for bulk savings";
        }
        if (combined.contains("double") || combined.contains("11") || combined.contains("双11")) {
            return "Shopping festival purchase - seasonal spending pattern";
        }
        if (combined.contains("red packet") || combined.contains("红包")) {
            return "Cultural gift exchange";
        }
        if (t.getAmount() % 100 == 0 && t.getAmount() >= 100) {
            return "Round number transaction - possibly planned expense";
        }
        return "";
    }

    /**
     * Generate financial analysis locally with Chinese context
     */
    private String generateLocalFinancialAnalysis(List<Transaction> transactions, YearMonth selectedMonth) {
        // If no month selected, use the most recent month
        YearMonth targetMonth = selectedMonth;
        if (targetMonth == null) {
            // Find the most recent month
            LocalDate mostRecent = LocalDate.MIN;
            for (Transaction t : transactions) {
                if (t.getDate().isAfter(mostRecent)) {
                    mostRecent = t.getDate();
                }
            }
            targetMonth = YearMonth.from(mostRecent);
        }

        // Filter transactions for the target month
        List<Transaction> monthlyTransactions = new ArrayList<>();
        for (Transaction t : transactions) {
            if (YearMonth.from(t.getDate()).equals(targetMonth)) {
                monthlyTransactions.add(t);
            }
        }

        // Calculate monthly income and expenses
        double totalIncome = 0;
        double totalExpense = 0;
        Map<String, Double> expenseByCategory = new HashMap<>();
        Map<String, Double> incomeByCategory = new HashMap<>();

        for (Transaction t : monthlyTransactions) {
            if (t.getType() == Transaction.Type.INCOME) {
                totalIncome += t.getAmount();
                incomeByCategory.merge(t.getCategory(), t.getAmount(), Double::sum);
            } else {
                totalExpense += t.getAmount();
                expenseByCategory.merge(t.getCategory(), t.getAmount(), Double::sum);
            }
        }

        // Find the top expense category
        String topExpenseCategory = "";
        double topExpenseAmount = 0;
        for (Map.Entry<String, Double> entry : expenseByCategory.entrySet()) {
            if (entry.getValue() > topExpenseAmount) {
                topExpenseAmount = entry.getValue();
                topExpenseCategory = entry.getKey();
            }
        }

        // Calculate savings rate
        double savingsRate = totalIncome > 0 ? (totalIncome - totalExpense) / totalIncome * 100 : 0;

        // Analyze Chinese-specific patterns
        boolean hasRedPackets = expenseByCategory.containsKey("Red Packet") || incomeByCategory.containsKey("Red Packet");
        boolean hasShoppingFestival = isShoppingFestivalMonth(targetMonth);
        double shoppingAmount = expenseByCategory.getOrDefault("Shopping", 0.0);

        // Generate analysis text with Chinese context
        StringBuilder analysis = new StringBuilder();

        // Title with seasonal context
        analysis.append("# Financial Analysis for ").append(targetMonth.getMonth().toString())
                .append(" ").append(targetMonth.getYear());

        String seasonalContext = getSeasonalContext(targetMonth);
        if (!seasonalContext.isEmpty()) {
            analysis.append(" (").append(seasonalContext).append(")");
        }
        analysis.append("\n\n");

        // 1. Monthly Summary with Chinese context
        analysis.append("## 1. Monthly Summary\n\n");
        analysis.append(String.format("- Total Income: ¥%.2f\n", totalIncome));
        analysis.append(String.format("- Total Expenses: ¥%.2f\n", totalExpense));
        analysis.append(String.format("- Net Savings: ¥%.2f (%.1f%% savings rate)\n\n",
                totalIncome - totalExpense, savingsRate));

        // Add Chinese context observations
        if (hasRedPackets) {
            analysis.append("Spring Festival Activity: Red packet transactions detected - traditional gift exchange period.\n");
        }
        if (hasShoppingFestival && shoppingAmount > 0) {
            analysis.append(String.format("Shopping Festival Impact: ¥%.2f spent on shopping during festival season.\n", shoppingAmount));
        }
        analysis.append("\n");

        // 2. Expense Analysis with Chinese context
        analysis.append("## 2. Expense Analysis with Chinese Context\n\n");
        if (!expenseByCategory.isEmpty()) {
            analysis.append("Your spending patterns this month:\n\n");

            List<Map.Entry<String, Double>> sortedExpenses = new ArrayList<>(expenseByCategory.entrySet());
            sortedExpenses.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            for (int i = 0; i < Math.min(5, sortedExpenses.size()); i++) {
                Map.Entry<String, Double> entry = sortedExpenses.get(i);
                double percentage = (entry.getValue() / totalExpense) * 100;
                analysis.append(String.format("- %s: ¥%.2f (%.1f%% of expenses)",
                        entry.getKey(), entry.getValue(), percentage));

                String context = getCategoryContext(entry.getKey(), targetMonth);
                if (!context.isEmpty()) {
                    analysis.append(" - ").append(context);
                }
                analysis.append("\n");
            }
            analysis.append("\n");
        }

        // 3. Income Sources
        if (!incomeByCategory.isEmpty()) {
            analysis.append("## 3. Income Sources\n\n");
            for (Map.Entry<String, Double> entry : incomeByCategory.entrySet()) {
                double percentage = (entry.getValue() / totalIncome) * 100;
                analysis.append(String.format("- %s: ¥%.2f (%.1f%% of income)\n",
                        entry.getKey(), entry.getValue(), percentage));
            }
            analysis.append("\n");
        }

        // 4. Chinese-Specific Financial Recommendations
        analysis.append("## 4. Chinese Context Financial Recommendations\n\n");

        if (hasShoppingFestival && shoppingAmount > totalIncome * 0.3) {
            analysis.append("Shopping Festival Alert: Your shopping expenses were significant during festival season.\n");
            analysis.append("Recommendation: Set a pre-festival budget and stick to it. Consider waiting 24 hours before making non-essential purchases.\n\n");
        }

        if (hasRedPackets) {
            analysis.append("Red Packet Strategy: Spring Festival gift exchanges detected.\n");
            analysis.append("Recommendation: Track red packet giving/receiving to maintain balanced social relationships while managing expenses.\n\n");
        }

        if (savingsRate < 10) {
            analysis.append("Savings Rate Below Ideal: Consider the Chinese tradition of saving at least 20-30% of income.\n");
            analysis.append("Recommendation: Explore Chinese savings products like Yu'e Bao or fixed deposits for steady growth.\n\n");
        } else if (savingsRate > 50) {
            analysis.append("Excellent Savings Rate: You're following strong Chinese saving traditions!\n");
            analysis.append("Recommendation: Consider diversifying into Chinese investment products or funds for better returns.\n\n");
        }

        // 5. Cultural Spending Insights
        analysis.append("## 5. Cultural Spending Insights\n\n");

        double foodExpense = expenseByCategory.getOrDefault("Food", 0.0);
        double transportExpense = expenseByCategory.getOrDefault("Transport", 0.0);
        double entertainmentExpense = expenseByCategory.getOrDefault("Entertainment", 0.0);

        if (foodExpense > 0) {
            analysis.append(String.format("Food Spending: ¥%.2f - ", foodExpense));
            if (targetMonth.getMonth().getValue() == 1 || targetMonth.getMonth().getValue() == 2) {
                analysis.append("Spring Festival reunion meals and treats are culturally important.\n");
            } else {
                analysis.append("Consider balancing dining out with home cooking for better savings.\n");
            }
        }

        if (entertainmentExpense > 0) {
            analysis.append(String.format("Entertainment: ¥%.2f - ", entertainmentExpense));
            analysis.append("Digital entertainment and social activities are part of modern Chinese lifestyle.\n");
        }

        // 6. Next Month Action Plan
        analysis.append("\n## 6. Action Plan for Next Month\n\n");
        analysis.append("1. Cultural Budgeting: Set specific budgets for seasonal festivals and social obligations.\n");

        if (hasShoppingFestival) {
            analysis.append("2. Festival Shopping: Plan ahead for next shopping festival with a dedicated budget.\n");
        } else {
            analysis.append("2. Maintain current spending discipline and look for optimization opportunities.\n");
        }

        analysis.append("3. Savings Goals: Aim for a 20-30% savings rate following Chinese financial wisdom.\n");
        analysis.append("4. Investment Consideration: Explore Chinese investment platforms for better returns on savings.\n\n");

        analysis.append("-\n");
        analysis.append("*This analysis includes Chinese cultural context and spending patterns. Enable API for more detailed AI insights.*");

        return analysis.toString();
    }

    /**
     * Check if the month is a shopping festival month
     */
    private boolean isShoppingFestivalMonth(YearMonth month) {
        int monthValue = month.getMonth().getValue();
        return monthValue == 6 || monthValue == 11; // 618 and Double 11
    }

    /**
     * Parse DeepSeek API response - Fixed version
     */
    private String parseDeepSeekResponse(String apiResponse) {
        try {
            // Find "content" field
            int contentStart = apiResponse.indexOf("\"content\":\"");
            if (contentStart == -1) {
                throw new RuntimeException("Content field not found in API response");
            }

            contentStart += 11; // Move to content start position (skip "content":"")

            // Find content end position (looking for next unescaped quote)
            int contentEnd = contentStart;
            boolean escaped = false;

            while (contentEnd < apiResponse.length()) {
                char c = apiResponse.charAt(contentEnd);

                if (c == '\\') {
                    // Skip escape character and character after it
                    escaped = !escaped;
                } else if (c == '"' && !escaped) {
                    // Found an unescaped quote, this is the end of content
                    break;
                } else {
                    escaped = false;
                }

                contentEnd++;
            }

            if (contentEnd >= apiResponse.length()) {
                throw new RuntimeException("Content end quote not found in API response");
            }

            // Extract content portion
            String content = apiResponse.substring(contentStart, contentEnd);

            // Process JSON escape characters
            content = content.replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\t", "\t")
                    .replace("\\r", "\r");

            return content;
        } catch (Exception e) {
            return "Error parsing API response: " + e.getMessage() + "\n\nRaw response: " + apiResponse;
        }
    }

    /**
     * Escape JSON string
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void showChart() {
        List<Transaction> transactions = controller.getAllTransactions();

        Set<YearMonth> availableMonths = new TreeSet<>();
        Set<String> allCategories = new TreeSet<>();
        for (Transaction t : transactions) {
            availableMonths.add(YearMonth.from(t.getDate()));
            allCategories.add(t.getCategory());
        }

        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");
        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");

        JFrame chartFrame = new JFrame("Transaction Charts & Smart Analysis");
        chartFrame.setLayout(new BorderLayout());
        chartFrame.setSize(1000, 700);  // Increased height for enhanced features
        chartFrame.setLocationRelativeTo(null);

        JPanel controlPanel = new JPanel(new FlowLayout());
        JComboBox<YearMonth> monthSelector = new JComboBox<>(availableMonths.toArray(new YearMonth[0]));
        JComboBox<Transaction.Type> typeSelector = new JComboBox<>(Transaction.Type.values());
        JComboBox<String> categorySelector = new JComboBox<>();
        categorySelector.addItem("All");
        for (String category : allCategories) categorySelector.addItem(category);

        controlPanel.add(new JLabel("Select Month:"));
        controlPanel.add(monthSelector);
        controlPanel.add(new JLabel("Select Type:"));
        controlPanel.add(typeSelector);
        controlPanel.add(new JLabel("Select Category:"));
        controlPanel.add(categorySelector);

        JPanel summaryPanel = new JPanel(new FlowLayout());
        JLabel incomeLabel = new JLabel("Total Income: ¥0.00");
        JLabel expenseLabel = new JLabel("Total Expense: ¥0.00");
        JLabel statusLabel = new JLabel("");
        double[] monthlyBudget = {2000.0};
        JLabel budgetLabel = new JLabel("Monthly Budget: ¥2000.00");

        JProgressBar budgetBar = new JProgressBar(0, 100);
        budgetBar.setStringPainted(true);
        JPanel budgetBarWrapper = new JPanel();
        budgetBar.setPreferredSize(new Dimension(300, 20));
        budgetBarWrapper.add(budgetBar);

        summaryPanel.add(incomeLabel);
        summaryPanel.add(expenseLabel);
        summaryPanel.add(budgetLabel);

        JLabel instruction = new JLabel("Double-click budget to edit");
        instruction.setFont(new Font("Dialog", Font.ITALIC, 11));
        instruction.setForeground(Color.GRAY);
        summaryPanel.add(instruction);

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(summaryPanel, BorderLayout.NORTH);
        infoPanel.add(budgetBarWrapper, BorderLayout.CENTER);
        infoPanel.add(statusLabel, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(controlPanel, BorderLayout.NORTH);
        topPanel.add(infoPanel, BorderLayout.SOUTH);
        chartFrame.add(topPanel, BorderLayout.NORTH);

        JPanel chartPanel = new JPanel(new GridLayout(1, 2));
        chartFrame.add(chartPanel, BorderLayout.CENTER);

        // Create Enhanced Notes section with Chinese context
        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Notes title and icon
        JPanel notesHeaderPanel = new JPanel(new BorderLayout());
        JPanel titleIconPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel notesIconLabel = new JLabel("🧠"); // Brain emoji for smart analysis
        notesIconLabel.setFont(new Font("Dialog", Font.PLAIN, 24));
        JLabel notesTitleLabel = new JLabel("Smart Financial Analysis");
        notesTitleLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        titleIconPanel.add(notesIconLabel);
        titleIconPanel.add(notesTitleLabel);

        JButton aiAnalysisButton = new JButton("🤖 AI Analysis (Chinese Context)");
        JButton saveNotesButton = new JButton("Save");
        JButton editNotesButton = new JButton("Edit");
        JCheckBox autoSaveCheckBox = new JCheckBox("Auto Save");
        autoSaveCheckBox.setSelected(true);

        // Add buttons to panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(autoSaveCheckBox);
        buttonsPanel.add(aiAnalysisButton);
        buttonsPanel.add(editNotesButton);
        buttonsPanel.add(saveNotesButton);

        // Combine title and buttons
        notesHeaderPanel.add(titleIconPanel, BorderLayout.WEST);
        notesHeaderPanel.add(buttonsPanel, BorderLayout.EAST);

        // Notes content area
        JTextArea notesTextArea = new JTextArea();
        notesTextArea.setLineWrap(true);
        notesTextArea.setWrapStyleWord(true);
        notesTextArea.setFont(new Font("Dialog", Font.PLAIN, 14));
        notesTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        notesTextArea.setEditable(false); // Initially set to non-editable

        // Load saved notes or use Chinese-context default
        String savedNotes = loadSavedNotes();
        if (savedNotes != null && !savedNotes.isEmpty()) {
            notesTextArea.setText(savedNotes);
        } else {
            // Enhanced default notes with Chinese context
            StringBuilder defaultNotes = new StringBuilder();
            defaultNotes.append("🛍️ SHOPPING FESTIVAL PREP\n");
            defaultNotes.append("- Double 11 (Nov 11) and 618 (June 18) are major shopping festivals\n");
            defaultNotes.append("- Set spending limits before these events to avoid overspending\n");
            defaultNotes.append("- Use price tracking tools to ensure real discounts\n\n");

            defaultNotes.append("🧧 SPRING FESTIVAL PLANNING\n");
            defaultNotes.append("- Budget for red packet giving during Chinese New Year\n");
            defaultNotes.append("- Plan for reunion dinner costs and family gifts\n");
            defaultNotes.append("- Save throughout the year for festival expenses\n\n");

            defaultNotes.append("📱 DIGITAL PAYMENT OPTIMIZATION\n");
            defaultNotes.append("- Take advantage of cashback from Alipay/WeChat Pay\n");
            defaultNotes.append("- Use financial apps for better money management\n");
            defaultNotes.append("- Monitor live streaming purchase impulses\n\n");

            defaultNotes.append("💰 INVESTMENT CONSIDERATIONS\n");
            defaultNotes.append("- Explore Chinese investment platforms for better returns\n");
            defaultNotes.append("- Consider traditional savings with steady growth\n");
            defaultNotes.append("- Balance between savings and quality of life spending");

            notesTextArea.setText(defaultNotes.toString());
        }

        JScrollPane notesScrollPane = new JScrollPane(notesTextArea);
        notesScrollPane.setPreferredSize(new Dimension(780, 150));

        // Enhanced button events
        saveNotesButton.addActionListener(e -> {
            saveNotes(notesTextArea.getText());
            notesTextArea.setEditable(false);
            editNotesButton.setText("Edit");
            JOptionPane.showMessageDialog(chartFrame, "Smart analysis notes saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        editNotesButton.addActionListener(e -> {
            if (notesTextArea.isEditable()) {
                notesTextArea.setEditable(false);
                editNotesButton.setText("Edit");
                if (autoSaveCheckBox.isSelected()) {
                    saveNotes(notesTextArea.getText());
                }
            } else {
                notesTextArea.setEditable(true);
                editNotesButton.setText("Done");
                notesTextArea.requestFocus();
            }
        });

        // Enhanced AI analysis button with Chinese context
        aiAnalysisButton.addActionListener(e -> {
            aiAnalysisButton.setEnabled(false);
            aiAnalysisButton.setText("🤖 Analyzing with Chinese Context...");

            final YearMonth selectedMonth = (YearMonth) monthSelector.getSelectedItem();

            SwingWorker<String, Void> analysisWorker = new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    try {
                        return generateFinancialAnalysisViaAPI(transactions, selectedMonth);
                    } catch (Exception ex) {
                        System.err.println("API analysis failed: " + ex.getMessage() + ". Using local analysis with Chinese context.");
                        return generateLocalFinancialAnalysis(transactions, selectedMonth);
                    }
                }

                @Override
                protected void done() {
                    try {
                        String analysis = get();
                        analysis = formatReportText(analysis);
                        notesTextArea.setText(analysis);
                        notesTextArea.setCaretPosition(0);

                        if (autoSaveCheckBox.isSelected()) {
                            saveNotes(analysis);
                        }

                        aiAnalysisButton.setEnabled(true);
                        aiAnalysisButton.setText("🤖 AI Analysis (Chinese Context)");

                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                                chartFrame,
                                "Error generating Chinese context analysis: " + ex.getMessage(),
                                "Analysis Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        aiAnalysisButton.setEnabled(true);
                        aiAnalysisButton.setText("🤖 AI Analysis (Chinese Context)");
                    }
                }
            };

            analysisWorker.execute();
        });

        // Enhanced styling for Chinese context
        Color enhancedBackgroundColor = new Color(235, 245, 255); // Light blue for modern feel
        notesPanel.setBackground(enhancedBackgroundColor);
        notesHeaderPanel.setBackground(enhancedBackgroundColor);
        titleIconPanel.setBackground(enhancedBackgroundColor);
        buttonsPanel.setBackground(enhancedBackgroundColor);
        notesTextArea.setBackground(new Color(250, 250, 255));
        autoSaveCheckBox.setBackground(enhancedBackgroundColor);

        notesPanel.add(notesHeaderPanel, BorderLayout.NORTH);
        notesPanel.add(notesScrollPane, BorderLayout.CENTER);

        // Create detail panel with enhanced context
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBackground(enhancedBackgroundColor);

        JTextArea detailArea = new JTextArea(5, 80);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailArea.setLineWrap(false);
        detailArea.setWrapStyleWord(false);
        detailArea.setEditable(false);
        detailArea.setBackground(new Color(250, 250, 255));

        JScrollPane scrollPane = new JScrollPane(detailArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(enhancedBackgroundColor);

        detailPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(chartPanel, BorderLayout.CENTER);
        mainContentPanel.add(detailPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainContentPanel, notesPanel);
        splitPane.setResizeWeight(0.7);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerSize(8);

        chartFrame.add(splitPane, BorderLayout.CENTER);

        // Enhanced budget functionality
        Runnable[] updateCharts = new Runnable[1];
        budgetLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String input = (String) JOptionPane.showInputDialog(
                            chartFrame,
                            "Enter new monthly budget amount (¥):",
                            "Set Budget",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            monthlyBudget[0]
                    );
                    if (input != null) {
                        try {
                            double newBudget = Double.parseDouble(input);
                            if (newBudget > 0) {
                                monthlyBudget[0] = newBudget;
                                budgetLabel.setText(String.format("Monthly Budget: ¥%.2f", newBudget));
                                updateCharts[0].run();
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(chartFrame, "Invalid number format.", "Input Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        // Enhanced chart update with Chinese context awareness
        updateCharts[0] = () -> {
            YearMonth selectedMonth = (YearMonth) monthSelector.getSelectedItem();
            Transaction.Type selectedType = (Transaction.Type) typeSelector.getSelectedItem();
            String selectedCategory = (String) categorySelector.getSelectedItem();

            if (selectedMonth == null || selectedType == null) return;

            List<Transaction> allThisMonth = new ArrayList<>();
            for (Transaction t : transactions) {
                if (YearMonth.from(t.getDate()).equals(selectedMonth)) {
                    allThisMonth.add(t);
                }
            }

            List<Transaction> filtered = new ArrayList<>();
            for (Transaction t : allThisMonth) {
                if (t.getType() == selectedType && ("All".equals(selectedCategory) || t.getCategory().equals(selectedCategory))) {
                    filtered.add(t);
                }
            }

            double totalIncome = allThisMonth.stream().filter(t -> t.getType() == Transaction.Type.INCOME).mapToDouble(Transaction::getAmount).sum();
            double totalExpense = allThisMonth.stream().filter(t -> t.getType() == Transaction.Type.EXPENSE).mapToDouble(Transaction::getAmount).sum();

            incomeLabel.setText(String.format("Total Income: ¥%.2f", totalIncome));
            expenseLabel.setText(String.format("Total Expense: ¥%.2f", totalExpense));

            // Enhanced budget analysis with Chinese context
            double rawPercent = (totalExpense / monthlyBudget[0]) * 100;
            int barValue = (int) Math.min(100, rawPercent);
            budgetBar.setValue(barValue);
            budgetBar.setString(String.format("%.1f%% of Budget Used", rawPercent));

            // Enhanced color coding with Chinese spending patterns
            if (rawPercent > 100) {
                budgetBar.setForeground(Color.RED);
            } else if (rawPercent > 80) {
                budgetBar.setForeground(Color.ORANGE);
            } else if (rawPercent < 50) {
                budgetBar.setForeground(Color.GREEN);
            } else {
                budgetBar.setForeground(Color.BLUE);
            }

            // Enhanced transaction details with Chinese context
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-12s | %-10s | %-35s | %-15s | Context%n", "Date", "Amount", "Note", "Source"));
            sb.append(String.format("%-12s | %-10s | %-35s | %-15s | -------%n", "------------", "----------", "-----------------------------------", "---------------"));

            for (Transaction t : filtered) {
                String context = getTransactionBehaviorContext(t);
                if (context.isEmpty()) context = "Regular";

                sb.append(String.format("%-12s | ¥%-9.2f | %-35s | %-15s | %s%n",
                        t.getDate(), t.getAmount(),
                        t.getNote().length() > 35 ? t.getNote().substring(0, 32) + "..." : t.getNote(),
                        t.getSource().length() > 15 ? t.getSource().substring(0, 12) + "..." : t.getSource(),
                        context));
            }

            detailArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            detailArea.setText(sb.toString());

            if (filtered.isEmpty()) {
                chartPanel.removeAll();
                statusLabel.setText("No transaction data for the selected month and type.");
                chartPanel.revalidate();
                chartPanel.repaint();
                return;
            }

            // Enhanced status with Chinese context
            String seasonalInfo = getSeasonalContext(selectedMonth);
            statusLabel.setText(seasonalInfo.isEmpty() ? "" : "📅 " + seasonalInfo);

            Map<String, Double> categoryTotals = new HashMap<>();
            for (Transaction t : filtered) {
                categoryTotals.merge(t.getCategory(), t.getAmount(), Double::sum);
            }

            DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                pieDataset.setValue(entry.getKey(), entry.getValue());
            }

            JFreeChart pieChart = ChartFactory.createPieChart(
                    selectedType + " by Category - " + selectedMonth +
                            (seasonalInfo.isEmpty() ? "" : " (" + seasonalInfo + ")"),
                    pieDataset, true, true, false);
            pieChart.setBackgroundPaint(Color.WHITE);
            pieChart.getPlot().setBackgroundPaint(Color.WHITE);
            pieChart.getPlot().setOutlinePaint(null);
            ChartPanel piePanel = new ChartPanel(pieChart);

            Map<LocalDate, Double> dailyTotal = new HashMap<>();
            for (Transaction t : filtered) {
                dailyTotal.merge(t.getDate(), t.getAmount(), Double::sum);
            }

            List<LocalDate> sortedDates = new ArrayList<>(dailyTotal.keySet());
            sortedDates.sort(Comparator.naturalOrder());

            TimeSeries series = new TimeSeries(selectedType + " Daily Trend");
            for (LocalDate date : sortedDates) {
                series.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()), dailyTotal.get(date));
            }

            TimeSeriesCollection dataset = new TimeSeriesCollection(series);
            JFreeChart lineChart = ChartFactory.createTimeSeriesChart(
                    selectedType + " Daily Trend - " + selectedMonth,
                    "Date", "Amount (¥)", dataset, true, true, false);
            lineChart.setBackgroundPaint(Color.WHITE);
            lineChart.getPlot().setBackgroundPaint(Color.WHITE);
            lineChart.getPlot().setOutlinePaint(null);
            XYPlot plot = lineChart.getXYPlot();
            DateAxis axis = (DateAxis) plot.getDomainAxis();
            axis.setDateFormatOverride(new SimpleDateFormat("MMM dd", Locale.ENGLISH));
            ChartPanel linePanel = new ChartPanel(lineChart);

            chartPanel.removeAll();
            chartPanel.add(piePanel);
            chartPanel.add(linePanel);
            chartPanel.revalidate();
            chartPanel.repaint();
        };

        monthSelector.addActionListener(e -> updateCharts[0].run());
        typeSelector.addActionListener(e -> updateCharts[0].run());
        categorySelector.addActionListener(e -> updateCharts[0].run());

        updateCharts[0].run();
        chartFrame.setVisible(true);
    }

    /**
     * Save user's financial notes with enhanced Chinese context
     */
    private void saveNotes(String notes) {
        try {
            File configDir = new File(System.getProperty("user.home"), ".personalfinance");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            File notesFile = new File(configDir, "smart_financial_notes.txt");

            try (FileWriter writer = new FileWriter(notesFile)) {
                // Add timestamp and context header
                writer.write("# Smart Financial Analysis Notes\n");
                writer.write("# Generated: " + LocalDate.now() + "\n");
                writer.write("# Context: Chinese Consumer Behavior Analysis\n\n");
                writer.write(notes);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error saving smart analysis notes: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Load user's saved financial notes with fallback
     */
    private String loadSavedNotes() {
        try {
            File configDir = new File(System.getProperty("user.home"), ".personalfinance");
            File notesFile = new File(configDir, "smart_financial_notes.txt");

            if (!notesFile.exists()) {
                // Try legacy file
                File legacyFile = new File(configDir, "financial_notes.txt");
                if (legacyFile.exists()) {
                    notesFile = legacyFile;
                } else {
                    return null;
                }
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(notesFile))) {
                String line;
                boolean skipHeader = true;
                while ((line = reader.readLine()) != null) {
                    // Skip header comments
                    if (skipHeader && line.startsWith("#")) {
                        continue;
                    }
                    skipHeader = false;
                    content.append(line).append("\n");
                }
            }

            return content.toString().trim();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel title = new JLabel("Smart Tracking Days:");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        daysLabel = new JLabel("0");
        daysLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        daysLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel daysText = new JLabel("days recorded");
        daysText.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(200, 10));
        separator.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tipTitle = new JLabel("Smart Tip:");
        tipTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        tipLabel = new JLabel("Loading...");
        tipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tipLabel.setMaximumSize(new Dimension(200, 60));
        tipLabel.setHorizontalAlignment(SwingConstants.CENTER);



        panel.add(title);
        panel.add(Box.createVerticalStrut(20));
        panel.add(daysLabel);
        panel.add(daysText);
        panel.add(Box.createVerticalStrut(20));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(10));
        panel.add(tipTitle);
        panel.add(Box.createVerticalStrut(5));
        panel.add(tipLabel);
        panel.add(Box.createVerticalStrut(15));


        return panel;
    }

    private void updateStatsPanel() {
        List<Transaction> transactions = controller.getAllTransactions();
        Set<String> uniqueEditDates = new HashSet<>();

        // Count unique recording dates
        for (Transaction t : transactions) {
            uniqueEditDates.add(t.getEditTime().toString());
        }

        daysLabel.setText(String.valueOf(uniqueEditDates.size()));

        // Enhanced tip system with Chinese context
        String today = LocalDate.now().toString();
        LocalDate currentDate = LocalDate.now();

        if (uniqueEditDates.contains(today)) {
            // Check for Chinese context
            int month = currentDate.getMonthValue();
            if (month == 11) {
                tipLabel.setText("<html><center>✅ Recorded today!<br/>🛍️ Double 11 season - watch spending!</center></html>");
            } else if (month == 6) {
                tipLabel.setText("<html><center>✅ Recorded today!<br/>🎉 618 festival active - budget wisely!</center></html>");
            } else if (month == 1 || month == 2) {
                tipLabel.setText("<html><center>✅ Recorded today!<br/>🧧 Spring Festival - track red packets!</center></html>");
            } else {
                tipLabel.setText("<html><center>✅ Great! You've recorded today!<br/>📊 Keep up the smart tracking!</center></html>");
            }
        } else {
            tipLabel.setText("<html><center>⚠️ Haven't recorded today!<br/>💡 Daily tracking improves AI insights</center></html>");
        }
    }
}