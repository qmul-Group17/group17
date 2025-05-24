package controller;

import model.Transaction;
import java.util.*;
import java.time.LocalDate;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.File;

/**
 * Transaction Categorizer - Integrates both keyword matching and machine learning classification methods
 * Enhanced with keywords from actual transaction data
 */
public class TransactionCategorizer {

    // Map to store user corrections
    private static final Map<String, String> userCorrectionHistory = new HashMap<>();

    // Flag to determine which classification mode to use
    private static boolean useSimpleMode = true;  // Default to using simple mode

    // Expense category keyword mappings - Enhanced with transaction patterns
    private static final Map<String, List<String>> expenseKeywords = new HashMap<>();

    // Income category keyword mappings - Enhanced with transaction patterns
    private static final Map<String, List<String>> incomeKeywords = new HashMap<>();

    /**
     * Initialize keyword mappings with exact patterns from transaction history
     */
    static {
        try {
            loadUserCorrectionHistory();
        } catch (Exception e) {
            System.err.println("Could not load user corrections: " + e.getMessage());
        }

        // Enhanced Expense category keywords - exactly matching your transaction data
        expenseKeywords.put("Food", Arrays.asList(
                "餐饮", "吃", "饭", "餐厅", "外卖", "食物", "超市", "菜", "水果", "零食",
                "food", "restaurant", "meal", "takeout", "supermarket", "snack",
                "drink", "drinks", "groceries", "snacks"
        ));

        expenseKeywords.put("Transport", Arrays.asList(
                "交通", "地铁", "公交", "车", "打车", "滴滴", "高铁", "火车", "飞机",
                "transportation", "subway", "bus", "car", "taxi", "train", "flight",
                "shared bike", "共享单车", "共享单车月卡"
        ));

        expenseKeywords.put("Shopping", Arrays.asList(
                "购物", "淘宝", "京东", "天猫", "衣服", "鞋子", "裤子", "服饰", "包",
                "shopping", "clothes", "shoes", "bags", "online shopping",
                "casual pants", "sweatpants", "sportswear"
        ));

        expenseKeywords.put("Health", Arrays.asList(
                "健康", "医疗", "药品", "医院", "诊所", "体检", "保险",
                "health", "medical", "medicine", "hospital", "clinic", "check-up", "insurance"
        ));

        expenseKeywords.put("Entertainment", Arrays.asList(
                "娱乐", "电影", "游戏", "音乐", "演唱会", "戏剧", "博物馆", "展览",
                "entertainment", "movies", "games", "music", "concert", "theater", "theatre",
                "museum", "exhibition", "movie ticket", "电影票", "dance show"
        ));

        expenseKeywords.put("Beauty", Arrays.asList(
                "美容", "护肤", "化妆", "发型", "美甲", "SPA",
                "beauty", "skincare", "makeup", "hairstyle", "manicure", "spa",
                "cosmetics", "haircut"
        ));

        expenseKeywords.put("Other", Arrays.asList(
                "其他", "杂项", "other", "miscellaneous", "laundry", "daily goods", "okay"
        ));

        // Enhanced Income category keywords - exactly matching your transaction data
        incomeKeywords.put("Transfer In", Arrays.asList(
                "转入", "转账", "收到转账", "transfer in", "received transfer",
                "transport subsidy", "travel refund"
        ));

        incomeKeywords.put("Salary", Arrays.asList(
                "薪资", "工资", "薪水", "奖金", "年终奖",
                "salary", "wages", "pay", "bonus", "year-end bonus", "monthly salary"
        ));

        incomeKeywords.put("Lend", Arrays.asList(
                "借出", "loan repaid", "money lent returned"
        ));

        incomeKeywords.put("Borrow", Arrays.asList(
                "借款", "贷款", "借入", "loan", "credit loan", "bank loan",
                "borrowed from friend", "borrowing"
        ));

        incomeKeywords.put("Other", Arrays.asList(
                "其他收入", "其他", "奖金", "赠与", "收入",
                "other income", "gift", "income", "refund", "unknown income",
                "freelance", "cash gift", "reimbursement", "reimbursement for meals", "snacks", "okay"
        ));
    }

    /**
     * Load user correction history from file
     */
    @SuppressWarnings("unchecked")
    private static void loadUserCorrectionHistory() {
        File file = new File("user_corrections.dat");
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                Map<String, String> loaded = (Map<String, String>) ois.readObject();
                userCorrectionHistory.clear();
                userCorrectionHistory.putAll(loaded);
                System.out.println("Loaded " + userCorrectionHistory.size() + " user correction records.");
            } catch (Exception e) {
                System.err.println("Failed to load user correction history: " + e.getMessage());
            }
        }
    }

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
        return !useSimpleMode;
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
        String note = transaction.getNote() != null ? transaction.getNote().toLowerCase() : "";
        String source = transaction.getSource() != null ? transaction.getSource().toLowerCase() : "";

        // Use source if note is empty
        String textToMatch = (note == null || note.trim().isEmpty()) ? source : note;

        if (textToMatch == null || textToMatch.trim().isEmpty()) {
            // If both are empty, default to "Other"
            return new Transaction(
                    transaction.getType(),
                    transaction.getType() == Transaction.Type.EXPENSE ? "Other" : "Other",
                    transaction.getAmount(),
                    transaction.getDate(),
                    transaction.getNote(),
                    transaction.getSource()
            );
        }

        Map<String, List<String>> keywordsMap =
                (transaction.getType() == Transaction.Type.EXPENSE) ? expenseKeywords : incomeKeywords;

        String category = findBestMatchingCategory(textToMatch, keywordsMap);

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
     * Find the best matching category based on keywords
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
                    // Exact match gets a higher score
                    if (text.equals(keyword.toLowerCase())) {
                        score += 3;
                    }
                    // Partial word boundary match
                    else if (text.matches(".*\\b" + keyword.toLowerCase() + "\\b.*")) {
                        score += 2;
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

        // If using advanced mode, also train the ML model
        if (!useSimpleMode && originalTransaction != null) {
            // Create a new transaction with the corrected category for learning
            Transaction correctedTransaction = new Transaction(
                    originalTransaction.getType(),
                    newCategory,
                    originalTransaction.getAmount(),
                    originalTransaction.getDate(),
                    originalTransaction.getNote(),
                    originalTransaction.getSource()
            );

            // Train the ML model with the corrected transaction
            MLTransactionCategorizer.learnFromTransaction(correctedTransaction);
        }

        // Save the user correction history to a file
        saveUserCorrectionHistory();
    }

    /**
     * Generate a descriptive key for a transaction
     */
    private static String getDescriptiveKey(Transaction transaction) {
        if (transaction == null) return "";

        // Combine key information to create a unique identifier
        String type = transaction.getType() != null ? transaction.getType().toString() : "";
        String source = transaction.getSource() != null ? transaction.getSource() : "";
        String note = transaction.getNote() != null ? transaction.getNote() : "";
        double amount = transaction.getAmount();

        return type + "|" + source + "|" + note + "|" + amount;
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