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
    private JLabel statusLabel; // 添加状态栏标签

    public MainFrame() {
        super("Personal Finance Tracker");

        controller = new TransactionController();
        controller.loadTransactions(); // 启动时加载数据

        setLayout(new BorderLayout());

        // 创建菜单栏
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        tableModel = new DefaultTableModel(new String[]{"Type", "Category", "Amount", "Date", "Note", "Source"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 使表格不可编辑
            }
        };
        table = new JTable(tableModel);

        // 添加表格双击事件，用于编辑分类
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击
                    int row = table.getSelectedRow();
                    int column = table.getSelectedColumn();

                    // 如果双击的是分类列
                    if (column == 1 && row >= 0) {
                        String currentType = (String) tableModel.getValueAt(row, 0);
                        String currentCategory = (String) tableModel.getValueAt(row, 1);

                        // 根据类型（收入/支出）显示不同的分类选项
                        String[] categories;
                        if ("INCOME".equals(currentType)) {
                            categories = new String[]{"Transfer In", "Salary", "Investment", "Red Packet In", "Borrow", "Receive", "Other"};
                        } else {
                            categories = new String[]{
                                    "Food", "Transport", "Shopping", "Health", "Travel", "Beauty", "Entertainment", "Transfer",
                                    "Housing", "Social", "Education", "Communication", "Red Packet", "Investment",
                                    "Lend", "Repayment", "Parenting", "Pet", "Other"
                            };
                        }

                        // 显示分类选择对话框
                        String newCategory = (String) JOptionPane.showInputDialog(
                                MainFrame.this,
                                "选择交易分类:",
                                "编辑分类",
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                categories,
                                currentCategory
                        );

                        if (newCategory != null && !newCategory.equals(currentCategory)) {
                            // 更新数据模型中的分类
                            controller.updateCategory(row, newCategory);
                            updateTable(); // 刷新表格
                            statusLabel.setText("已将交易 #" + (row+1) + " 的分类更新为: " + newCategory);
                        }
                    }
                }
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        // 创建工具栏
        JToolBar toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        JButton addBtn = new JButton("Add Transaction");
        addBtn.addActionListener(e -> {
            TransactionDialog dialog = new TransactionDialog(this);
            dialog.setVisible(true);
            if (dialog.isSubmitted()) {
                Transaction t = dialog.getTransaction();
                // 使用分类器自动分类
                Transaction categorizedT = TransactionCategorizer.categorize(t);
                controller.addTransaction(categorizedT);
                updateTable();
                statusLabel.setText("已添加新交易并自动分类为: " + categorizedT.getCategory());
            }
        });

        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e -> {
            TableModel model = table.getModel();
            try (FileWriter writer = new FileWriter("output.csv")) {
                // 写入列名（标题）到CSV文件
                for (int i = 0; i < model.getColumnCount(); i++) {
                    writer.append(model.getColumnName(i));
                    if (i < model.getColumnCount() - 1) {
                        writer.append(','); // 列分隔符，除了最后一列外
                    } else {
                        writer.append('\n'); // 最后一列后换行
                    }
                }
                // 写入数据行到CSV文件
                for (int i = 0; i < model.getRowCount(); i++) {
                    for (int j = 0; j < model.getColumnCount(); j++) {
                        writer.append(String.valueOf(model.getValueAt(i, j))); // 将值转换为字符串并写入CSV文件
                        if (j < model.getColumnCount() - 1) {
                            writer.append(','); // 列分隔符，除了最后一列外
                        } else {
                            writer.append('\n'); // 最后一列后换行，换行表示新的一行数据开始
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
                // 在导入前对所有交易进行自动分类
                List<Transaction> categorizedImports = TransactionCategorizer.categorizeAll(imported);
                controller.importTransactions(categorizedImports);
                updateTable();
                statusLabel.setText("已导入并自动分类 " + imported.size() + " 条交易记录");
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(exportBtn);
        buttonPanel.add(importBtn);
        JButton chartBtn = new JButton("View Chart");//graph
        chartBtn.addActionListener(e -> showChart());
        buttonPanel.add(chartBtn);

        // 添加AI分类按钮
        JButton recategorizeBtn = new JButton("重新分类所有交易");
        recategorizeBtn.addActionListener(e -> {
            controller.recategorizeAll();
            updateTable();
            statusLabel.setText("已使用AI重新分类所有交易");
        });
        buttonPanel.add(recategorizeBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        // 添加状态栏
        statusLabel = new JLabel("就绪 - 使用 " +
                (TransactionCategorizer.isUsingAdvancedMode() ? "高级模式" : "关键词匹配") +
                " 进行分类" + (AppConfig.isUseAPI() ? " (API已启用)" : ""));
           statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(statusLabel, BorderLayout.NORTH);

        updateTable(); // 初始加载数据到表格
        setSize(900, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("文件");
        JMenuItem importItem = new JMenuItem("导入CSV");
        importItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                List<Transaction> imported = CSVImporter.importFromCSV(filePath);
                // 在导入前对所有交易进行自动分类
                List<Transaction> categorizedImports = TransactionCategorizer.categorizeAll(imported);
                controller.importTransactions(categorizedImports);
                updateTable();
                statusLabel.setText("已导入并自动分类 " + imported.size() + " 条交易记录");
            }
        });

        JMenuItem saveItem = new JMenuItem("保存数据");
        saveItem.addActionListener(e -> {
            controller.saveTransactions();
            statusLabel.setText("数据已保存");
        });

        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> {
            controller.saveTransactions();
            System.exit(0);
        });

        fileMenu.add(importItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // 编辑菜单
        JMenu editMenu = new JMenu("编辑");
        JMenuItem addItem = new JMenuItem("添加交易");
        addItem.addActionListener(e -> {
            TransactionDialog dialog = new TransactionDialog(this);
            dialog.setVisible(true);
            if (dialog.isSubmitted()) {
                Transaction t = dialog.getTransaction();
                // 使用分类器自动分类
                Transaction categorizedT = TransactionCategorizer.categorize(t);
                controller.addTransaction(categorizedT);
                updateTable();
                statusLabel.setText("已添加新交易并自动分类为: " + categorizedT.getCategory());
            }
        });

        editMenu.add(addItem);

        JMenu aiMenu = new JMenu("AI功能");
        JMenuItem toggleAIItem = new JMenuItem("切换分类模式");
        toggleAIItem.addActionListener(e -> {
            TransactionCategorizer.toggleMode();
            boolean isAdvancedMode = TransactionCategorizer.isUsingAdvancedMode(); // 修改方法名
            statusLabel.setText("当前分类模式: " + (isAdvancedMode ? "高级模式" : "关键词匹配"));
        });

        JMenuItem recategorizeItem = new JMenuItem("重新分类所有交易");
        recategorizeItem.addActionListener(e -> {
            controller.recategorizeAll();
            updateTable();
            statusLabel.setText("已重新分类所有交易");
        });

// 添加API设置选项
        JMenuItem apiSettingsItem = new JMenuItem("AI分类API设置");
        apiSettingsItem.addActionListener(e -> {
            APISettingsDialog dialog = new APISettingsDialog(this);
            dialog.setVisible(true);
        });

        aiMenu.add(toggleAIItem);
        aiMenu.add(recategorizeItem);
        aiMenu.addSeparator(); // 分隔线
        aiMenu.add(apiSettingsItem);


        // 图表菜单
        JMenu chartMenu = new JMenu("图表");
        JMenuItem viewChartItem = new JMenuItem("查看图表");
        viewChartItem.addActionListener(e -> showChart());
        chartMenu.add(viewChartItem);

        // 帮助菜单
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "个人财务管理系统 v1.0\n" +
                            "包含AI自动分类功能\n\n" +
                            "提示：双击表格中的分类单元格可以手动修改分类",
                    "关于",
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

        JButton addButton = new JButton("添加");
        addButton.addActionListener(e -> {
            TransactionDialog dialog = new TransactionDialog(this);
            dialog.setVisible(true);
            if (dialog.isSubmitted()) {
                Transaction t = dialog.getTransaction();
                // 使用分类器自动分类
                Transaction categorizedT = TransactionCategorizer.categorize(t);
                controller.addTransaction(categorizedT);
                updateTable();
            }
        });

        JButton importButton = new JButton("导入");
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

        JButton chartButton = new JButton("图表");
        chartButton.addActionListener(e -> showChart());

        JButton toggleModeButton = new JButton("切换AI模式");
        toggleModeButton.addActionListener(e -> {
            TransactionCategorizer.toggleMode();
            boolean isAdvancedMode = TransactionCategorizer.isUsingAdvancedMode();
            statusLabel.setText("当前分类模式: " +
                    (isAdvancedMode ? "高级模式" : "关键词匹配") +
                    (AppConfig.isUseAPI() ? " (API已启用)" : ""));
        });
        // 添加交易按钮处理程序
        addButton.addActionListener(e -> {
            TransactionDialog dialog = new TransactionDialog(this);
            dialog.setVisible(true);
            if (dialog.isSubmitted()) {
                Transaction t = dialog.getTransaction();
                // 使用控制器的分类方法，而不是直接调用分类器
                controller.addAndCategorizeTransaction(t);
                updateTable();
                statusLabel.setText("已添加新交易并自动分类为: " + t.getCategory());
            }
        });

// 导入CSV按钮处理程序
        importButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                // 使用控制器的导入方法，它会处理分类
                controller.importFromCSV(filePath);
                updateTable();
                statusLabel.setText("已导入并自动分类交易记录");
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
        lineChart.setBackgroundPaint(Color.WHITE); // 整个图表白底
        lineChart.getPlot().setBackgroundPaint(Color.WHITE); // 折线区域白底
        lineChart.getPlot().setOutlinePaint(null); // 去掉边框线（可选）
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
