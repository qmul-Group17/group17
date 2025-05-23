package controller;

import model.Transaction;
import java.util.*;
import java.time.LocalDate;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;


/**
 * 交易分类器 - 整合关键词匹配和机器学习两种分类方式
 */
public class TransactionCategorizer {

    // 在类的顶部添加这个变量
    private static final Map<String, String> userCorrectionHistory = new HashMap<>();

    /**
     * 保存用户修正历史
     */
    public static void saveUserCorrectionHistory() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("user_corrections.dat"))) {
            oos.writeObject(userCorrectionHistory);
            System.out.println("已保存 " + userCorrectionHistory.size() + " 条用户修正记录");
        } catch (Exception e) {
            System.err.println("保存用户修正历史失败: " + e.getMessage());
        }
    }

    // 支出类别关键词映射
    private static final Map<String, List<String>> expenseKeywords = new HashMap<>();

    // 收入类别关键词映射
    private static final Map<String, List<String>> incomeKeywords = new HashMap<>();

    // 分类模式
    private static boolean useSimpleMode = true;  // 默认使用简单模式

    // 初始化关键词映射
    static {
        // 支出分类关键词
        expenseKeywords.put("Food", Arrays.asList("餐饮", "吃", "饭", "餐厅", "外卖", "食物", "超市", "菜", "水果", "零食"));
        expenseKeywords.put("Transport", Arrays.asList("交通", "地铁", "公交", "车", "打车", "滴滴", "高铁", "火车", "飞机"));
        expenseKeywords.put("Shopping", Arrays.asList("购物", "淘宝", "京东", "天猫", "衣服", "鞋子", "裤子", "服饰", "包"));
        // 添加其他分类的关键词...

        // 收入分类关键词
        incomeKeywords.put("Transfer In", Arrays.asList("收转账", "转入", "收到转账"));
        incomeKeywords.put("Salary", Arrays.asList("薪资", "工资", "薪水", "奖金", "年终奖"));
        // 添加其他分类的关键词...
    }

    /**
     * 切换分类模式
     */
    public static void toggleMode() {
        useSimpleMode = !useSimpleMode;
    }

    /**
     * 获取当前分类模式
     * @return true表示使用高级模式，false表示使用简单模式
     */
    public static boolean isUsingAdvancedMode() {
        return !isUsingSimpleMode(); // 或者直接修改内部逻辑来匹配新方法名
    }

    /**
     * 获取当前分类模式
     * @return true表示使用简单模式，false表示使用机器学习模式
     */
    public static boolean isUsingSimpleMode() {
        return useSimpleMode;
    }

    /**
     * 分类交易
     * @param transaction 需要分类的交易
     * @return 分类后的交易
     */
    // 在 TransactionCategorizer.java 中修改 categorize 方法
    public static Transaction categorize(Transaction transaction) {
        // 首先检查用户修正历史
        String key = getDescriptiveKey(transaction);
        if (userCorrectionHistory.containsKey(key)) {
            String correctedCategory = userCorrectionHistory.get(key);
            return new Transaction(
                    transaction.getType(),
                    correctedCategory,
                    transaction.getAmount(),
                    transaction.getDate(),
                    transaction.getNote(),
                    transaction.getSource()
            );
        }

        if (useSimpleMode) {
            return categorizeWithKeywords(transaction);
        } else {
            // 判断是否使用API
            if (AppConfig.isUseAPI()) {
                // 调用MLTransactionCategorizer进行分类
                String predictedCategory = MLTransactionCategorizer.predictCategory(transaction);
                return new Transaction(
                        transaction.getType(),
                        predictedCategory,
                        transaction.getAmount(),
                        transaction.getDate(),
                        transaction.getNote(),
                        transaction.getSource()
                );
            } else {
                // 没有API则回退到关键词分类
                return categorizeWithKeywords(transaction);
            }
        }
    }

    /**
     * 使用关键词匹配方式分类交易
     */
    private static Transaction categorizeWithKeywords(Transaction transaction) {
        String note = transaction.getNote().toLowerCase();
        if (note == null || note.trim().isEmpty()) {
            note = transaction.getSource().toLowerCase();
        }

        Map<String, List<String>> keywordsMap =
                (transaction.getType() == Transaction.Type.EXPENSE) ? expenseKeywords : incomeKeywords;

        String category = findBestMatchingCategory(note, keywordsMap);

        // 创建新的Transaction对象，保持原始数据不变
        return new Transaction(
                transaction.getType(),
                category,
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getNote(),
                transaction.getSource()
        );
    }

    /**
     * 查找最匹配的分类
     */
    private static String findBestMatchingCategory(String text, Map<String, List<String>> keywordsMap) {
        Map<String, Integer> scores = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : keywordsMap.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();

            int score = 0;
            for (String keyword : keywords) {
                if (text.contains(keyword.toLowerCase())) {
                    score++;
                    if (text.equals(keyword.toLowerCase())) {
                        score += 3;
                    }
                }
            }

            if (score > 0) {
                scores.put(category, score);
            }
        }

        if (scores.isEmpty()) {
            return "Other";
        }

        return Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    /**
     * 记录用户修正的分类
     */
    public static void recordUserCorrection(Transaction originalTransaction, String newCategory) {
        String key = getDescriptiveKey(originalTransaction);
        userCorrectionHistory.put(key, newCategory);

        // 保存用户修正历史到文件
        saveUserCorrectionHistory();
    }

    /**
     * 生成交易的描述性键
     */
    private static String getDescriptiveKey(Transaction transaction) {
        // 组合关键信息创建唯一标识
        return transaction.getType() + "|" +
                transaction.getSource() + "|" +
                transaction.getNote() + "|" +
                transaction.getAmount();
    }

    /**
     * 批量分类交易
     */
    public static List<Transaction> categorizeAll(List<Transaction> transactions) {
        List<Transaction> categorizedTransactions = new ArrayList<>();

        for (Transaction transaction : transactions) {
            categorizedTransactions.add(categorize(transaction));
        }

        return categorizedTransactions;
    }
}