package view;

import controller.CSVImporter;
import controller.TransactionController;
import model.Transaction;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

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
}
