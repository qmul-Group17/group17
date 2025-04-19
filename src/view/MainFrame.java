package view;

import controller.CSVImporter;
import controller.TransactionController;
import model.Transaction;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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


public class MainFrame extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private TransactionController controller;

    public MainFrame() {
        super("Personal Finance Tracker");

        controller = new TransactionController();
        controller.loadTransactions(); // 启动时加载数据

        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(new String[]{"Type", "Category", "Amount", "Date", "Note", "Source"}, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);


        
        JButton addBtn = new JButton("Add Transaction");
        addBtn.addActionListener(e -> {
            TransactionDialog dialog = new TransactionDialog(this);
            dialog.setVisible(true);
            if (dialog.isSubmitted()) {
                Transaction t = dialog.getTransaction();
                controller.addTransaction(t);
                updateTable();
            }
        });

        JButton importBtn = new JButton("Import CSV");
        importBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                List<Transaction> imported = CSVImporter.importFromCSV(filePath);
                controller.importTransactions(imported); // 使用新的导入方法
                updateTable();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(importBtn);
        JButton chartBtn = new JButton("View Chart");//graph
        chartBtn.addActionListener(e -> showChart());
        buttonPanel.add(chartBtn);  
        add(buttonPanel, BorderLayout.SOUTH);

        updateTable(); // 初始加载数据到表格
        setSize(800, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

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