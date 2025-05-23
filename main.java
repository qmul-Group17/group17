package main.java;

import controller.TransactionController;
import controller.BudgetController;
import controller.CategorizationController;
import controller.TransactionCategorizer;
import controller.MLTransactionCategorizer;
import controller.AppConfig;
import controller.CSVImporter;
import view.MainFrame;
import view.LoginFrame;
import model.Transaction;
import model.Budget;
import model.UserPreferences;
import util.FileUtils;
import util.AIUtils;

public class Main {

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            new LoginFrame(); // Show login/register first
        });
        // 初始化用户偏好设置
        UserPreferences userPreferences = new UserPreferences();

        // 初始化预算和交易模型
        Budget budget = new Budget();
        Transaction transaction = new Transaction();

        // 控制器初始化
        TransactionController transactionController = new TransactionController();
        transactionController.loadTransactions(); // 加载已有交易

        BudgetController budgetController = new BudgetController(budget);
        CategorizationController categorizationController = new CategorizationController(transaction);

        // 初始化交易分类器
        TransactionCategorizer.saveUserCorrectionHistory();

        // 启动GUI界面
        MainFrame mainFrame = new MainFrame();
        mainFrame.setVisible(true);

        // 读取资源文件（如 CSV）
        try {
            FileUtils.loadCSV("resources/sample.csv");
        } catch (Exception e) {
            System.out.println("示例CSV文件不存在，跳过加载");
        }

        // 如果需要 AI 自动分类功能
        try {
            AIUtils.autoCategorize(transaction);
        } catch (Exception e) {
            System.out.println("AI自动分类初始化失败：" + e.getMessage());
        }

        // 设置一些初始的用户偏好（如果有）
        userPreferences.loadPreferences();

        // 其他初始化工作
        System.out.println("个人财务管理系统启动成功！");
        System.out.println("分类模式: " + (TransactionCategorizer.isUsingSimpleMode() ? "简单模式" : "高级模式"));
        System.out.println("API状态: " + (AppConfig.isUseAPI() ? "已启用" : "未启用"));
    }
}