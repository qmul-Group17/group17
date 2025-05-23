package controller;

import model.Transaction;
import java.util.*;
import java.time.LocalDate;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;


/**
 * Transaction Categorizer - Integrates both keyword matching and machine learning classification methods
 */
public class TransactionCategorizer {

    // Add this variable at the top of the class to store user corrections
    private static final Map<String, String> userCorrectionHistory = new HashMap<>();

    /**
     * Save user correction history
     */
    public static void saveUserCorrectionHistory() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("user_corrections.dat"))) {
            oos.writeObject(userCorrectionHistory);
            System.out.println("Saved " + userCorrectionHistory.size() + " user correction records.");
        } catch (Exception e) {
            System.err.println("Failed to save user correction history: " + e.getMessage());
        }
    }

    // Expense category keyword mappings
    private static final Map<String, List<String>> expenseKeywords = new HashMap<>();

    // Income category keyword mappings
    private static final Map<String, List<String>> incomeKeywords = new HashMap<>();

    // Classification mode
    private static boolean useSimpleMode = true;  // Default to using simple mode

    // Initialize keyword mappings
    static {
        // Expense category keywords
        expenseKeywords.put("Food", Arrays.asList("餐饮", "吃", "饭", "餐厅", "外卖", "食物", "超市", "菜", "水果", "零食", "food", "restaurant", "meal", "takeout", "supermarket", "snack"));
        expenseKeywords.put("Transport", Arrays.asList("交通", "地铁", "公交", "车", "打车", "滴滴", "高铁", "火车", "飞机", "transportation", "subway", "bus", "car", "taxi", "high-speed train", "train", "flight"));
        expenseKeywords.put("Shopping", Arrays.asList("购物", "淘宝", "京东", "天猫", "衣服", "鞋子", "裤子", "服饰", "包", "shopping", "clothes", "shoes", "bags", "online shopping"));
        expenseKeywords.put("Health", Arrays.asList("健康", "医疗", "药品", "医院", "诊所", "体检", "保险", "health", "medical", "medicine", "hospital", "clinic", "check-up", "insurance"));
        expenseKeywords.put("Travel", Arrays.asList("旅行", "旅游", "机票", "酒店", "景点", "导游", "travel", "tourism", "flight", "hotel", "sightseeing", "tour guide"));
        expenseKeywords.put("Beauty", Arrays.asList("美容", "护肤", "化妆", "发型", "美甲", "SPA", "beauty", "skincare", "makeup", "hairstyle", "manicure", "spa"));
        expenseKeywords.put("Entertainment", Arrays.asList("娱乐", "电影", "游戏", "音乐", "演唱会", "戏剧", "博物馆", "展览", "entertainment", "movies", "games", "music", "concert", "theater", "museum", "exhibition"));
        expenseKeywords.put("Housing", Arrays.asList("房租", "物业费", "房贷", "水电费", "家居", "装修", "rent", "property fee", "mortgage", "utilities", "furniture", "renovation"));
        expenseKeywords.put("Social", Arrays.asList("社交", "聚会", "朋友", "聚餐", "生日", "结婚", "团建", "social", "party", "friends", "dinner", "birthday", "wedding", "team-building"));
        expenseKeywords.put("Education", Arrays.asList("学习", "书籍", "学费", "培训", "课程", "在线学习", "education", "books", "tuition", "training", "course", "online learning"));

        // Income category keywords
        incomeKeywords.put("Transfer In", Arrays.asList("收转账", "转入", "收到转账", "transfer in", "received transfer"));
        incomeKeywords.put("Salary", Arrays.asList("薪资", "工资", "薪水", "奖金", "年终奖", "salary", "wages", "pay", "bonus", "year-end bonus"));
        incomeKeywords.put("Investment", Arrays.asList("投资", "股票", "基金", "债券", "投资收益", "股票红利", "理财", "investment", "stocks", "fund", "bonds", "investment returns", "stock dividend", "wealth management"));
        incomeKeywords.put("Red Packet In", Arrays.asList("红包", "礼金", "节日红包", "转账", "红包收款", "red packet", "gift money", "holiday red packet", "transfer", "received red packet"));
        incomeKeywords.put("Borrowing", Arrays.asList("借款", "贷款", "信用贷款", "银行借款", "loan", "credit loan", "bank loan"));
        incomeKeywords.put("Receipt", Arrays.asList("收入", "款项", "回款", "打款", "存款", "income", "payment", "received payment", "deposit"));
        incomeKeywords.put("Other", Arrays.asList("其他收入", "奖金", "赠与", "收入", "other income", "bonus", "gift", "income"));
    }

    /**
     * Toggle classification mode
     */
    public static void toggleMode() {
        useSimpleMode = !useSimpleMode;
    }

    /**
     * Get current classification mode
     * @return true if using advanced mode, false if using simple mode
     */
    public static boolean isUsingAdvancedMode() {
        return !isUsingSimpleMode(); // Or simply modify internal logic to match the new method name
    }

    /**
     * Get current classification mode
     * @return true if using simple mode, false if using machine learning mode
     */
    public static boolean isUsingSimpleMode() {
        return useSimpleMode;
    }

    /**
     * Classify a transaction
     * @param transaction The transaction to classify
     * @return The classified transaction
     */
    public static Transaction categorize(Transaction transaction) {
        // First, check the user correction history
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
            // Check if API is being used
            if (AppConfig.isUseAPI()) {
                // Call MLTransactionCategorizer for classification
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
                // No API, fall back to keyword-based classification
                return categorizeWithKeywords(transaction);
            }
        }
    }

    /**
     * Classify a transaction using keyword matching
     */
    private static Transaction categorizeWithKeywords(Transaction transaction) {
        String note = transaction.getNote().toLowerCase();
        if (note == null || note.trim().isEmpty()) {
            note = transaction.getSource().toLowerCase();
        }

        Map<String, List<String>> keywordsMap =
                (transaction.getType() == Transaction.Type.EXPENSE) ? expenseKeywords : incomeKeywords;

        String category = findBestMatchingCategory(note, keywordsMap);

        // Create a new Transaction object, keeping the original data intact
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
     * Find the best matching category
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
     * Record a user correction for a transaction's category
     */
    public static void recordUserCorrection(Transaction originalTransaction, String newCategory) {
        String key = getDescriptiveKey(originalTransaction);
        userCorrectionHistory.put(key, newCategory);

        // Save the user correction history to a file
        saveUserCorrectionHistory();
    }

    /**
     * Generate a descriptive key for a transaction
     */
    private static String getDescriptiveKey(Transaction transaction) {
        // Combine key information to create a unique identifier
        return transaction.getType() + "|" +
                transaction.getSource() + "|" +
                transaction.getNote() + "|" +
                transaction.getAmount();
    }

    /**
     * Classify multiple transactions in bulk
     */
    public static List<Transaction> categorizeAll(List<Transaction> transactions) {
        List<Transaction> categorizedTransactions = new ArrayList<>();

        for (Transaction transaction : transactions) {
            categorizedTransactions.add(categorize(transaction));
        }

        return categorizedTransactions;
    }
}