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

// 在MainFrame类顶部添加导入：
import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;


public class MainFrame extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private TransactionController controller;
    private JLabel statusLabel; // Add status bar label
    private User currentUser;
    private JLabel daysLabel;
    private JLabel tipLabel;


    public MainFrame(User user) {// In MainFrame constructor
        super("Personal Finance Tracker");
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
        JButton recategorizeBtn = new JButton("Recategorize All Transactions");
        recategorizeBtn.addActionListener(e -> {
            controller.recategorizeAll();
            updateTable();

            // 根据当前的分类模式显示不同的状态消息
            if (TransactionCategorizer.isUsingSimpleMode()) {
                statusLabel.setText("All transactions recategorized using keyword matching");
            } else {
                // 检查是否启用了API
                if (AppConfig.isUseAPI()) {
                    statusLabel.setText("All transactions recategorized using AI classification");
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
                updateStatsPanel();
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

    /**
     * Convert report text, replacing bold markers (asterisks) with uppercase text
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
     * Generate financial analysis report using DeepSeek API
     *
     * @param transactions List of transactions
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

            // Build API request
            StringBuilder prompt = new StringBuilder();
            prompt.append("As a financial analyst, review the following monthly transaction data and create a comprehensive financial analysis report with actionable recommendations. ");
            prompt.append("The report should include monthly summary, expense analysis, income sources analysis, unusual expenses, personalized financial recommendations, and an action plan for next month.\n\n");

            // Add month information
            prompt.append("Month: ").append(targetMonth.getMonth()).append(" ").append(targetMonth.getYear()).append("\n\n");

            // Add overall financial summary
            prompt.append("FINANCIAL SUMMARY:\n");
            prompt.append("- Total Income: ¥").append(String.format("%.2f", totalIncome)).append("\n");
            prompt.append("- Total Expenses: ¥").append(String.format("%.2f", totalExpense)).append("\n");
            prompt.append("- Net Savings: ¥").append(String.format("%.2f", totalIncome - totalExpense)).append("\n");
            prompt.append("- Savings Rate: ").append(String.format("%.1f%%", totalIncome > 0 ? (totalIncome - totalExpense) / totalIncome * 100 : 0)).append("\n\n");

            // Add expense categories breakdown
            prompt.append("EXPENSE CATEGORIES:\n");
            List<Map.Entry<String, Double>> sortedExpenses = new ArrayList<>(expenseByCategory.entrySet());
            sortedExpenses.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            for (Map.Entry<String, Double> entry : sortedExpenses) {
                double percentage = totalExpense > 0 ? (entry.getValue() / totalExpense) * 100 : 0;
                prompt.append("- ").append(entry.getKey()).append(": ¥").append(String.format("%.2f", entry.getValue()))
                        .append(" (").append(String.format("%.1f%%", percentage)).append(")\n");
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

            // Add top 5 largest expenses
            prompt.append("TOP 5 LARGEST EXPENSES:\n");
            List<Transaction> sortedByAmount = new ArrayList<>(monthlyTransactions);
            sortedByAmount.removeIf(t -> t.getType() == Transaction.Type.INCOME);
            sortedByAmount.sort((t1, t2) -> Double.compare(t2.getAmount(), t1.getAmount()));

            for (int i = 0; i < Math.min(5, sortedByAmount.size()); i++) {
                Transaction t = sortedByAmount.get(i);
                prompt.append("- ").append(t.getCategory()).append(": ¥").append(String.format("%.2f", t.getAmount()))
                        .append(" on ").append(t.getDate()).append(" (").append(t.getNote()).append(")\n");
            }
            prompt.append("\n");

// Update prompts, create user-friendly financial report
            prompt.append("FORMAT REQUIREMENTS:\n");
            prompt.append("1. Create a friendly, easy-to-read financial report with practical advice\n");
            prompt.append("2. Use normal sentence case - NO ALL CAPS text anywhere in the report\n");
            prompt.append("3. Keep language simple and avoid financial jargon when possible\n");
            prompt.append("4. Focus on helpful insights and actionable recommendations rather than excessive analysis\n");
            prompt.append("5. Use short paragraphs and bullet points for readability\n");
            prompt.append("6. Include a brief summary at the top with 2-3 key takeaways\n");
            prompt.append("7. For any recommendation, explain briefly WHY it would be helpful\n");
            prompt.append("8. DO NOT use any markdown formatting like asterisks (**) for bold text\n");

            prompt.append("\nCONTENT REQUIREMENTS:\n");
            prompt.append("1. Keep the report concise - aim for clarity over completeness\n");
            prompt.append("2. Focus 70% on practical recommendations and 30% on analysis\n");
            prompt.append("3. Highlight unusual patterns but suggest practical solutions\n");
            prompt.append("4. Use everyday examples to explain financial concepts\n");
            prompt.append("5. Include a short, practical action plan with 3-4 specific steps\n");
            prompt.append("6. Mention 1-2 potential ways to improve financial health next month\n");
            prompt.append("7. Congratulate the user on positive aspects (like high savings)\n");
            prompt.append("\nEXAMPLE FORMAT TO FOLLOW:\n\n");
            prompt.append("OCTOBER 2024 FINANCIAL REPORT 💰\n\n");
            prompt.append("- Exceptional savings rate (97%)—your income far exceeds expenses this month\n");
            prompt.append("- Spending is concentrated in shopping/entertainment (70% of expenses)\n");
            prompt.append("- Unusual pattern: Extremely low living costs suggest either atypical month or under-tracking\n\n");
            prompt.append("---\n\n");
            prompt.append("MONTHLY SUMMARY 📊\n\n");
            prompt.append("- Income: ¥27,418 (55% salary, 45% other sources)\n");
            prompt.append("- Expenses: ¥826 (just 3% of income)\n");
            prompt.append("- Net savings: ¥26,592—far above the recommended 20-30% savings rate\n\n");
            prompt.append("What's working well:\n");
            prompt.append("- High income with diverse sources (salary + other)\n");
            prompt.append("- Minimal essential spending (food/transport under 10% of expenses)\n\n");
            prompt.append("---\n\n");
            prompt.append("EXPENSE ANALYSIS 🔍\n\n");
            prompt.append("Priority areas:\n");
            prompt.append("1. Shopping/Entertainment/Beauty (90% of expenses):\n");
            prompt.append("   - Concert tickets (¥287) and skincare (¥165) are discretionary but reasonable\n");
            prompt.append("   - Casual pants (¥294) seems high for one item—consider budgeting for clothing\n\n");
            prompt.append("2. Low essentials:\n");
            prompt.append("   - Food (¥56) and transport (¥20) are unusually low\n");
            prompt.append("   - If this is typical, great job! If not, check for missing transactions\n\n");
            prompt.append("---\n\n");
            prompt.append("INCOME SOURCES 💼\n\n");
            prompt.append("- Salary (55%): Stable and predictable income base\n");
            prompt.append("- Other (45%): Significant but unspecified source\n\n");
            prompt.append("If this \"other\" includes bonuses or side income, consider investing or setting aside consistently for future goals.\n\n");
            prompt.append("---\n\n");
            prompt.append("SMART RECOMMENDATIONS ✅\n\n");
            prompt.append("1. Verify your savings rate:\n");
            prompt.append("   Why it matters: While impressive, ensure you're not missing regular expenses\n");
            prompt.append("   If accurate, this puts you in the top 1% of savers nationwide\n\n");
            prompt.append("2. Create a balanced budget:\n");
            prompt.append("   Recommendation: Allocate a \"fun money\" fund (5-10% of income = ¥1,400-2,700)\n");
            prompt.append("   This prevents feeling deprived while maintaining excellent saving habits\n\n");
            prompt.append("3. Put your savings to work:\n");
            prompt.append("   Consider moving part of your savings into investments to beat inflation\n");
            prompt.append("   Even a conservative 4% return would add ¥1,060 to your wealth annually\n\n");
            prompt.append("---\n\n");
            prompt.append("ACTION PLAN FOR NOVEMBER 🎯\n\n");
            prompt.append("1. Verify essentials: Double-check if rent/utilities are missing from tracking\n");
            prompt.append("2. Set a \"fun budget\": Limit shopping/entertainment to ¥2,000–3,000 per month\n");
            prompt.append("3. Invest surplus: Open a low-risk investment account for ¥10,000\n");
            prompt.append("4. Track \"other income\": Label this source clearly for better future planning\n\n");
            prompt.append("---\n\n");
            prompt.append("FINAL NOTE 🚀\n\n");
            prompt.append("You're doing exceptionally well with savings! Just ensure your spending aligns with\n");
            prompt.append("your priorities. Even small tweaks can make your already strong financial habits\n");
            prompt.append("even more sustainable for the long term.\n\n");
            prompt.append("Report generated based on October 2024 data. Keep up the great work!\n\n");

            prompt.append("\nIMPORTANT CONTENT GUIDELINES:\n");
            prompt.append("1. Focus on being positive and encouraging while still providing realistic advice\n");
            prompt.append("2. Include tangible, specific recommendations with actual numbers based on the data\n");
            prompt.append("3. Each recommendation should explain WHY it matters, not just what to do\n");
            prompt.append("4. Highlight unusual patterns but avoid negative judgments\n");
            prompt.append("5. Keep language simple and avoid financial jargon\n");
            prompt.append("6. Ensure that all advice is actionable (things the user can actually do)\n");
            prompt.append("7. The report should feel personal and tailored to this specific financial situation\n");

// Add explicit guidance for the comparison section
            prompt.append("\nCOMPARISON REQUIREMENTS:\n");
            prompt.append("1. Compare current month data with available metrics (e.g., typical spending patterns, recommended financial ratios)\n");
            prompt.append("2. Use standard financial benchmarks for comparison (savings rate targets, expense-to-income ratios)\n");
            prompt.append("3. Compare expense categories against each other to highlight priority areas\n");
            prompt.append("4. No need to request historical data - use only the information provided\n");
            prompt.append("5. If making comparisons to typical financial patterns, state this clearly without suggesting more data is needed\n");

            prompt.append("\nDATA UTILIZATION:\n");
            prompt.append("1. Your analysis should fully utilize all available transaction data for the month\n");
            prompt.append("2. Income and expense figures provided are comprehensive and authoritative\n");
            prompt.append("3. If specific categories seem underrepresented (e.g., low food costs), analyze this as user behavior, not missing data\n");
            prompt.append("4. Treat the extraordinarily high savings rate as factual, providing relevant advice based on this unusual pattern\n");
            prompt.append("5. All category allocations (shopping, entertainment, etc.) should be treated as accurate and complete\n");

            // Build API request body
            String requestBody = "{"
                    + "\"model\": \"deepseek-chat\","
                    + "\"messages\": ["
                    + "  {\"role\": \"user\", \"content\": \"" + escapeJson(prompt.toString()) + "\"}"
                    + "],"
                    + "\"temperature\": 0.2,"
                    + "\"max_tokens\": 2000"
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

    /**
     * Generate financial analysis locally (backup method when API is unavailable)
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

        // Calculate this month's savings rate
        double savingsRate = totalIncome > 0 ? (totalIncome - totalExpense) / totalIncome * 100 : 0;

        // Find unusual expenses
        List<Transaction> unusualExpenses = new ArrayList<>();
        // Calculate average expense
        double avgExpense = totalExpense / Math.max(1, monthlyTransactions.size());

        for (Transaction t : monthlyTransactions) {
            if (t.getType() == Transaction.Type.EXPENSE && t.getAmount() > avgExpense * 3) {
                unusualExpenses.add(t);
            }
        }

        // Generate analysis text
        StringBuilder analysis = new StringBuilder();

        // Title
        analysis.append("# Financial Analysis for ").append(targetMonth.getMonth().toString())
                .append(" ").append(targetMonth.getYear()).append("\n\n");

        // 1. Income and expense summary
        analysis.append("## 1. Monthly Summary\n\n");
        analysis.append(String.format("- Total Income: ¥%.2f\n", totalIncome));
        analysis.append(String.format("- Total Expenses: ¥%.2f\n", totalExpense));
        analysis.append(String.format("- Net Savings: ¥%.2f (%.1f%% of income)\n\n",
                totalIncome - totalExpense, savingsRate));

        // 2. Main expense analysis
        analysis.append("## 2. Expense Analysis\n\n");

        if (!expenseByCategory.isEmpty()) {
            analysis.append("Your top spending categories this month:\n\n");

            // Expense categories sorted by amount
            List<Map.Entry<String, Double>> sortedExpenses = new ArrayList<>(expenseByCategory.entrySet());
            sortedExpenses.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            for (int i = 0; i < Math.min(3, sortedExpenses.size()); i++) {
                Map.Entry<String, Double> entry = sortedExpenses.get(i);
                double percentage = (entry.getValue() / totalExpense) * 100;
                analysis.append(String.format("- %s: ¥%.2f (%.1f%% of expenses)\n",
                        entry.getKey(), entry.getValue(), percentage));
            }

            analysis.append("\n");
        }

        // 3. Income analysis
        if (!incomeByCategory.isEmpty()) {
            analysis.append("## 3. Income Sources\n\n");

            List<Map.Entry<String, Double>> sortedIncome = new ArrayList<>(incomeByCategory.entrySet());
            sortedIncome.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            for (int i = 0; i < Math.min(3, sortedIncome.size()); i++) {
                Map.Entry<String, Double> entry = sortedIncome.get(i);
                double percentage = (entry.getValue() / totalIncome) * 100;
                analysis.append(String.format("- %s: ¥%.2f (%.1f%% of income)\n",
                        entry.getKey(), entry.getValue(), percentage));
            }

            analysis.append("\n");
        }

        // 4. Unusual expense alerts
        if (!unusualExpenses.isEmpty()) {
            analysis.append("## 4. Unusual Expenses\n\n");
            analysis.append("The following expenses were significantly higher than your average:\n\n");

            for (Transaction t : unusualExpenses) {
                analysis.append(String.format("- %s: ¥%.2f on %s (%s)\n",
                        t.getCategory(), t.getAmount(), t.getDate().toString(), t.getNote()));
            }

            analysis.append("\n");
        }

        // 5. Financial advice
        analysis.append("## 5. Financial Recommendations\n\n");

        // Provide personalized recommendations based on different situations
        if (savingsRate < 0) {
            analysis.append("⚠️ **Warning**: Your expenses exceeded your income this month. Consider the following actions:\n\n");
            analysis.append("1. Review your spending in " + topExpenseCategory + ", which was your largest expense category.\n");
            analysis.append("2. Create a budget for next month to avoid overspending.\n");
            analysis.append("3. Look for additional income sources or reduce non-essential expenses.\n\n");
        } else if (savingsRate < 10) {
            analysis.append("⚠️ Your savings rate is lower than the recommended 10-20%. Consider the following:\n\n");
            analysis.append("1. Try to reduce spending in " + topExpenseCategory + " next month.\n");
            analysis.append("2. Set up automatic transfers to a savings account.\n");
            analysis.append("3. Review your subscriptions and recurring expenses.\n\n");
        } else if (savingsRate > 50) {
            analysis.append("🎉 Excellent saving rate! Some suggestions for your savings:\n\n");
            analysis.append("1. Consider investing part of your savings for long-term growth.\n");
            analysis.append("2. Make sure you have an emergency fund covering 3-6 months of expenses.\n");
            analysis.append("3. Consider setting financial goals for major future expenses.\n\n");
        } else {
            analysis.append("✅ Your finances look healthy! Here are some general tips:\n\n");
            analysis.append("1. Continue monitoring your " + topExpenseCategory + " spending.\n");
            analysis.append("2. Consider setting specific savings goals for major purchases.\n");
            analysis.append("3. Review your income sources and look for opportunities to diversify.\n\n");
        }

        // 6. Next month's action plan
        analysis.append("## 6. Action Plan for Next Month\n\n");
        analysis.append("1. Set a budget for each spending category, especially for " + topExpenseCategory + ".\n");

        if (savingsRate < 20) {
            analysis.append("2. Aim to increase your savings rate to at least 20% of income.\n");
        } else {
            analysis.append("2. Maintain your good saving habits and consider investing strategies.\n");
        }

        if (unusualExpenses.isEmpty()) {
            analysis.append("3. Continue your disciplined spending habits - no unusual expenses this month!\n");
        } else {
            analysis.append("3. Plan ahead for large expenses to avoid unexpected financial strain.\n");
        }

        analysis.append("4. Review your financial goals and track your progress regularly.\n\n");

        // 7. End note
        analysis.append("_This analysis was generated locally as the AI API was not available. For more personalized insights, enable the API in settings._");

        return analysis.toString();
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
        // FIXED: Using consistent panel naming and correct button initialization order

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

        JFrame chartFrame = new JFrame("Transaction Charts");
        chartFrame.setLayout(new BorderLayout());
        chartFrame.setSize(800, 650);  // Increase height to accommodate Notes section
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

        // Create Notes section
        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Notes title and icon
        JPanel notesHeaderPanel = new JPanel(new BorderLayout());
        JPanel titleIconPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel notesIconLabel = new JLabel("\uD83D\uDCDD"); // Using Unicode paper icon
        notesIconLabel.setFont(new Font("Dialog", Font.PLAIN, 24));
        JLabel notesTitleLabel = new JLabel("Notes");
        notesTitleLabel.setFont(new Font("Dialog", Font.BOLD, 20));
        titleIconPanel.add(notesIconLabel);
        titleIconPanel.add(notesTitleLabel);

        // FIXED: Define aiAnalysisButton before using it
        JButton aiAnalysisButton = new JButton("AI Analysis");
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

        // Load saved notes from settings or database, if none use default value
        String savedNotes = loadSavedNotes();

        if (savedNotes != null && !savedNotes.isEmpty()) {
            notesTextArea.setText(savedNotes);
        } else {
            // Default note items
            StringBuilder defaultNotes = new StringBuilder();
            defaultNotes.append("1. The end-of-month shopping festival is coming. Make a shopping list in advance to avoid impulsive buying.\n\n");
            defaultNotes.append("2. Your gym membership is about to expire. Evaluate how often you use it before deciding whether to renew it.\n\n");
            defaultNotes.append("3. When the new semester starts for your child, buy stationery and tutoring materials as needed. Don't stock up blindly.\n\n");
            defaultNotes.append("4. It's fruit season. Buy fresh fruits in moderation to prevent wasting money on fruits that go bad before you can finish them.");

            notesTextArea.setText(defaultNotes.toString());
        }

        JScrollPane notesScrollPane = new JScrollPane(notesTextArea);
        notesScrollPane.setPreferredSize(new Dimension(780, 150));

        // Button events
        saveNotesButton.addActionListener(e -> {
            saveNotes(notesTextArea.getText());
            notesTextArea.setEditable(false);
            editNotesButton.setText("Edit");
            JOptionPane.showMessageDialog(chartFrame, "Notes saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        editNotesButton.addActionListener(e -> {
            if (notesTextArea.isEditable()) {
                // If currently editable, switch back to read-only
                notesTextArea.setEditable(false);
                editNotesButton.setText("Edit");

                // If auto-save is checked, save the content
                if (autoSaveCheckBox.isSelected()) {
                    saveNotes(notesTextArea.getText());
                }
            } else {
                // If currently read-only, switch to editable
                notesTextArea.setEditable(true);
                editNotesButton.setText("Done");
                notesTextArea.requestFocus();
            }
        });

        // AI analysis button event handler
        aiAnalysisButton.addActionListener(e -> {
            // Show progress indicator
            aiAnalysisButton.setEnabled(false);
            aiAnalysisButton.setText("Analyzing...");

            // Get currently selected month
            final YearMonth selectedMonth = (YearMonth) monthSelector.getSelectedItem();

            // Create and start AI analysis task
            SwingWorker<String, Void> analysisWorker = new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    try {
                        // Try to generate analysis using API
                        return generateFinancialAnalysisViaAPI(transactions, selectedMonth);
                    } catch (Exception ex) {
                        // If API call fails, fall back to local analysis
                        System.err.println("API analysis failed: " + ex.getMessage() + ". Falling back to local analysis.");
                        return generateLocalFinancialAnalysis(transactions, selectedMonth);
                    }
                }

                @Override
                protected void done() {
                    try {
                        // Get analysis result
                        String analysis = get();

                        // Convert bold markers to uppercase text
                        analysis = formatReportText(analysis);

                        // Update text area
                        notesTextArea.setText(analysis);
                        notesTextArea.setCaretPosition(0); // Scroll to top

                        // Auto-save analysis result (if enabled)
                        if (autoSaveCheckBox.isSelected()) {
                            saveNotes(analysis);
                        }

                        // Reset button state
                        aiAnalysisButton.setEnabled(true);
                        aiAnalysisButton.setText("AI Analysis");

                    } catch (Exception ex) {
                        // Handle error
                        JOptionPane.showMessageDialog(
                                chartFrame,
                                "Error generating analysis: " + ex.getMessage(),
                                "Analysis Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        aiAnalysisButton.setEnabled(true);
                        aiAnalysisButton.setText("AI Analysis");
                    }
                }
            };

            // Start analysis task
            analysisWorker.execute();
        });

        // Add title and content to Notes panel
        notesPanel.add(notesHeaderPanel, BorderLayout.NORTH);
        notesPanel.add(notesScrollPane, BorderLayout.CENTER);

        // Set Notes panel background color to light purple, similar to screenshot
        Color notesBackgroundColor = new Color(240, 230, 255);
        notesPanel.setBackground(notesBackgroundColor);
        notesHeaderPanel.setBackground(notesBackgroundColor);
        titleIconPanel.setBackground(notesBackgroundColor);
        buttonsPanel.setBackground(notesBackgroundColor);
        notesTextArea.setBackground(new Color(245, 240, 255));
        autoSaveCheckBox.setBackground(notesBackgroundColor);

        // Add Notes panel to chart window bottom
        chartFrame.add(notesPanel, BorderLayout.SOUTH);

        // Transaction details panel moved to middle area bottom
        // Create detail panel
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBackground(new Color(240, 230, 255)); // match Notes section

// Create detail area and scroll
        JTextArea detailArea = new JTextArea(5, 80); // trigger scroll after 3 lines
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailArea.setLineWrap(false); // disable auto wrap
        detailArea.setWrapStyleWord(false);
        detailArea.setEditable(false);
        detailArea.setBackground(new Color(245, 240, 255)); // light purple background

        JScrollPane scrollPane = new JScrollPane(detailArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(new Color(240, 230, 255));

        detailPanel.add(scrollPane, BorderLayout.CENTER);


        // Create middle main content panel, including charts and details
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.add(chartPanel, BorderLayout.CENTER);
        mainContentPanel.add(detailPanel, BorderLayout.SOUTH);

        // Add main content panel to chart window middle
        chartFrame.add(mainContentPanel, BorderLayout.CENTER);

        Runnable[] updateCharts = new Runnable[1];
        budgetLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String input = (String) JOptionPane.showInputDialog(
                            chartFrame,                                      // parent
                            "Enter new budget amount (RMB):",                // message
                            "Set Budget",                                    // title ✅ 你要显示的英文标题
                            JOptionPane.PLAIN_MESSAGE,                       // message type
                            null,                                            // icon
                            null,                                            // selectionValues
                            monthlyBudget[0]                                 // initial value
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
                            JOptionPane.showMessageDialog(chartFrame, "Invalid number.", "Input Error", JOptionPane.INFORMATION_MESSAGE);

                        }
                    }
                }
            }
        });

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

            double rawPercent = (totalExpense / monthlyBudget[0]) * 100;
            int barValue = (int) Math.min(100, rawPercent);
            budgetBar.setValue(barValue);
            budgetBar.setString(String.format("Expense %.1f%% of Budget", rawPercent));
            budgetBar.setForeground(rawPercent > 100 ? Color.RED : Color.BLUE);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-12s | %-10s | %-35s | %-15s%n", "Date", "Amount", "Note", "Source"));
            sb.append(String.format("%-12s | %-10s | %-35s | %-15s%n", "------------", "----------", "-----------------------------------", "---------------"));
            for (Transaction t : filtered) {
                sb.append(String.format("%-12s | ¥%-9.2f | %-35s | %-15s%n",
                        t.getDate(), t.getAmount(), t.getNote(), t.getSource()));
            }
            detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            detailArea.setText(sb.toString());

            if (filtered.isEmpty()) {
                chartPanel.removeAll();
                statusLabel.setText("No transaction data for the selected month and type.");
                chartPanel.revalidate();
                chartPanel.repaint();
                return;
            }

            statusLabel.setText("");
            Map<String, Double> categoryTotals = new HashMap<>();
            for (Transaction t : filtered) {
                categoryTotals.merge(t.getCategory(), t.getAmount(), Double::sum);
            }

            DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                pieDataset.setValue(entry.getKey(), entry.getValue());
            }

            JFreeChart pieChart = ChartFactory.createPieChart(selectedType + " by Category - " + selectedMonth, pieDataset, true, true, false);
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

            TimeSeries series = new TimeSeries(selectedType + " Daily");
            for (LocalDate date : sortedDates) {
                series.add(new Day(date.getDayOfMonth(), date.getMonthValue(), date.getYear()), dailyTotal.get(date));
            }

            TimeSeriesCollection dataset = new TimeSeriesCollection(series);
            JFreeChart lineChart = ChartFactory.createTimeSeriesChart(selectedType + " Trend - " + selectedMonth, "Date", "Amount", dataset, true, true, false);
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
     * Save user's financial notes
     * @param notes Notes content to save
     */
    private void saveNotes(String notes) {
        try {
            // Create user config directory (if it doesn't exist)
            File configDir = new File(System.getProperty("user.home"), ".personalfinance");
            if (!configDir.exists()) {
                configDir.mkdirs();
            }

            // Create notes file
            File notesFile = new File(configDir, "financial_notes.txt");

            // Write notes content
            try (FileWriter writer = new FileWriter(notesFile)) {
                writer.write(notes);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error saving notes: " + e.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Load user's saved financial notes
     * @return Saved notes content, or null if none
     */
    private String loadSavedNotes() {
        try {
            File configDir = new File(System.getProperty("user.home"), ".personalfinance");
            File notesFile = new File(configDir, "financial_notes.txt");

            if (!notesFile.exists()) {
                return null;
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(notesFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }

            return content.toString();
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

        JLabel title = new JLabel("You have accumulated accounting:");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        daysLabel = new JLabel("0");
        daysLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        daysLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel daysText = new JLabel("days");
        daysText.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(200, 10));
        separator.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel tipTitle = new JLabel("Tip:");
        tipTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        tipLabel = new JLabel("Loading...");
        tipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        tipLabel.setMaximumSize(new Dimension(200, 50));
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

        return panel;
    }

    private void updateStatsPanel() {
        List<Transaction> transactions = controller.getAllTransactions();
        Set<String> uniqueEditDates = new HashSet<>();

        for (Transaction t : transactions) {
            uniqueEditDates.add(t.getEditTime().toString());
        }

        daysLabel.setText(String.valueOf(uniqueEditDates.size()));

        String today = LocalDate.now().toString();
        if (uniqueEditDates.contains(today)) {
            tipLabel.setText("<html><center>✅ You've recorded today!</center></html>");
        } else {
            tipLabel.setText("<html><center>⚠️ You haven't recorded today!</center></html>");
        }
    }
}