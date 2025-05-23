package main.java;

import controller.TransactionController;
import controller.BudgetController;
import controller.CategorizationController;
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
        TransactionController transactionController = new TransactionController(transaction);
        BudgetController budgetController = new BudgetController(budget);
        CategorizationController categorizationController = new CategorizationController(transaction);

        // 启动GUI界面
        MainFrame mainFrame = new MainFrame(transactionController, budgetController, categorizationController);
        mainFrame.setVisible(true);

        // 读取资源文件（如 CSV）
        FileUtils.loadCSV("resources/sample.csv");

        // 如果需要 AI 自动分类功能
        AIUtils.autoCategorize(transaction);

        // 设置一些初始的用户偏好（如果有）
        userPreferences.loadPreferences();
        
        // 其他初始化工作
        System.out.println("应用启动成功！");
    }
}
