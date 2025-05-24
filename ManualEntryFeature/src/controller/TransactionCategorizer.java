package controller;

import model.Transaction;
import java.util.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.DayOfWeek;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.regex.Pattern;

/**
 * Enhanced Transaction Categorizer with Smart Chinese Context Recognition
 * Combines your existing logic with enhanced Chinese lifestyle recognition
 */
public class TransactionCategorizer {

    // Map to store user corrections
    private static final Map<String, String> userCorrectionHistory = new HashMap<>();

    // Flag to determine which classification mode to use
    private static boolean useSimpleMode = true;  // Default to using simple mode

    // Enhanced Expense category keyword mappings
    private static final Map<String, List<String>> expenseKeywords = new HashMap<>();

    // Enhanced Income category keyword mappings
    private static final Map<String, List<String>> incomeKeywords = new HashMap<>();

    // Smart pattern recognition for Chinese lifestyle
    private static final Map<Pattern, String> smartPatterns = new HashMap<>();
    private static final Map<Pattern, Double> confidenceScores = new HashMap<>();

    // Seasonal context
    private static final Map<Month, List<String>> seasonalContext = new HashMap<>();

    /**
     * Initialize keyword mappings with enhanced Chinese context
     */
    static {
        try {
            loadUserCorrectionHistory();
        } catch (Exception e) {
            System.err.println("Could not load user corrections: " + e.getMessage());
        }

        // Enhanced Expense category keywords - keeping your original + Chinese enhancements
        expenseKeywords.put("Food", Arrays.asList(
                "餐饮", "吃", "饭", "餐厅", "外卖", "食物", "超市", "菜", "水果", "零食",
                "food", "restaurant", "meal", "takeout", "supermarket", "snack",
                "drink", "drinks", "groceries", "snacks",
                // Chinese food culture enhancements
                "火锅", "烧烤", "奶茶", "咖啡", "麦当劳", "肯德基", "必胜客", "星巴克",
                "hotpot", "bbq", "bubble tea", "milk tea", "coffee", "starbucks", "mcdonald", "kfc", "pizza",
                "美团", "饿了么", "盒马", "叮咚", "meituan", "eleme", "delivery", "grocery delivery"
        ));

        expenseKeywords.put("Transport", Arrays.asList(
                "交通", "地铁", "公交", "车", "打车", "滴滴", "高铁", "火车", "飞机",
                "transportation", "subway", "bus", "car", "taxi", "train", "flight",
                "shared bike", "共享单车", "共享单车月卡",
                // Chinese transport enhancements
                "滴滴出行", "嘀嗒", "曹操", "摩拜", "哈啰", "青桔", "didi", "mobike", "hellobike",
                "高德", "百度地图", "充电桩", "停车费", "过路费", "etc", "parking", "toll"
        ));

        expenseKeywords.put("Shopping", Arrays.asList(
                "购物", "淘宝", "京东", "天猫", "衣服", "鞋子", "裤子", "服饰", "包",
                "shopping", "clothes", "shoes", "bags", "online shopping",
                "casual pants", "sweatpants", "sportswear",
                // Chinese e-commerce enhancements
                "拼多多", "小红书", "抖音", "快手", "直播", "带货", "团购", "拼团", "秒杀",
                "pinduoduo", "xiaohongshu", "douyin", "live streaming", "group buying", "flash sale",
                "双11", "双12", "618", "99", "black friday", "shopping festival", "优惠券", "满减"
        ));

        expenseKeywords.put("Health", Arrays.asList(
                "健康", "医疗", "药品", "医院", "诊所", "体检", "保险",
                "health", "medical", "medicine", "hospital", "clinic", "check-up", "insurance"
        ));

        expenseKeywords.put("Entertainment", Arrays.asList(
                "娱乐", "电影", "游戏", "音乐", "演唱会", "戏剧", "博物馆", "展览",
                "entertainment", "movies", "games", "music", "concert", "theater", "theatre",
                "museum", "exhibition", "movie ticket", "电影票", "dance show",
                // Chinese entertainment enhancements
                "王者荣耀", "和平精英", "原神", "腾讯", "网易", "steam", "b站", "爱奇艺",
                "腾讯视频", "优酷", "bilibili", "iqiyi", "youku", "ktv", "剧本杀", "密室",
                "vip", "会员", "membership", "subscription"
        ));

        expenseKeywords.put("Beauty", Arrays.asList(
                "美容", "护肤", "化妆", "发型", "美甲", "SPA",
                "beauty", "skincare", "makeup", "hairstyle", "manicure", "spa",
                "cosmetics", "haircut",
                // Chinese beauty enhancements
                "完美日记", "花西子", "理发", "染发", "烫发", "面膜", "精华", "屈臣氏", "丝芙兰"
        ));

        expenseKeywords.put("Other", Arrays.asList(
                "其他", "杂项", "other", "miscellaneous", "laundry", "daily goods", "okay"
        ));

        // Enhanced Income category keywords
        incomeKeywords.put("Transfer In", Arrays.asList(
                "转入", "转账", "收到转账", "transfer in", "received transfer",
                "transport subsidy", "travel refund"
        ));

        incomeKeywords.put("Red Packet", Arrays.asList(
                "红包", "收红包", "压岁钱", "过年红包", "新年红包", "spring festival red packet",
                "red packet", "lucky money", "new year money", "cash gift"
        ));

        incomeKeywords.put("Salary", Arrays.asList(
                "薪资", "工资", "薪水", "奖金", "年终奖", "提成", "加班费",
                "salary", "wages", "pay", "bonus", "year-end bonus", "monthly salary",
                "commission", "incentive", "overtime pay"
        ));

        incomeKeywords.put("Lend", Arrays.asList(
                "借出", "loan repaid", "money lent returned"
        ));

        incomeKeywords.put("Borrow", Arrays.asList(
                "借款", "贷款", "借入", "loan", "credit loan", "bank loan",
                "borrowed from friend", "borrowing"
        ));

        incomeKeywords.put("Other", Arrays.asList(
                "其他收入", "其他", "奖金", "赠与", "收入", "退款", "报销", "补贴",
                "other income", "gift", "income", "refund", "unknown income",
                "freelance", "兼职", "副业", "part-time", "side hustle",
                "cash gift", "reimbursement", "reimbursement for meals", "snacks", "okay", "subsidy"
        ));

        // Initialize smart patterns for Chinese lifestyle
        initializeSmartPatterns();
        initializeSeasonalContext();
    }

    /**
     * Initialize smart pattern recognition for Chinese lifestyle
     */
    private static void initializeSmartPatterns() {
        // Shopping festival patterns
        smartPatterns.put(Pattern.compile(".*(双1[12]|618|black friday).*", Pattern.CASE_INSENSITIVE), "Shopping");
        confidenceScores.put(Pattern.compile(".*(双1[12]|618|black friday).*", Pattern.CASE_INSENSITIVE), 0.9);

        // Live streaming shopping
        smartPatterns.put(Pattern.compile(".*(直播.*带货|live.*stream.*shop).*", Pattern.CASE_INSENSITIVE), "Shopping");
        confidenceScores.put(Pattern.compile(".*(直播.*带货|live.*stream.*shop).*", Pattern.CASE_INSENSITIVE), 0.85);

        // Group buying
        smartPatterns.put(Pattern.compile(".*(团购|拼团|group.*buy).*", Pattern.CASE_INSENSITIVE), "Shopping");
        confidenceScores.put(Pattern.compile(".*(团购|拼团|group.*buy).*", Pattern.CASE_INSENSITIVE), 0.8);

        // Red packet patterns
        smartPatterns.put(Pattern.compile(".*(春节.*红包|过年.*红包|new.*year.*red).*", Pattern.CASE_INSENSITIVE), "Red Packet");
        confidenceScores.put(Pattern.compile(".*(春节.*红包|过年.*红包|new.*year.*red).*", Pattern.CASE_INSENSITIVE), 0.95);

        // Wedding/ceremony patterns
        smartPatterns.put(Pattern.compile(".*(结婚.*礼金|wedding.*gift|份子钱).*", Pattern.CASE_INSENSITIVE), "Red Packet");
        confidenceScores.put(Pattern.compile(".*(结婚.*礼金|wedding.*gift|份子钱).*", Pattern.CASE_INSENSITIVE), 0.9);

        // Gaming patterns
        smartPatterns.put(Pattern.compile(".*(王者.*荣耀|和平.*精英|原神|steam).*", Pattern.CASE_INSENSITIVE), "Entertainment");
        confidenceScores.put(Pattern.compile(".*(王者.*荣耀|和平.*精英|原神|steam).*", Pattern.CASE_INSENSITIVE), 0.9);
    }

    /**
     * Initialize seasonal context
     */
    private static void initializeSeasonalContext() {
        seasonalContext.put(Month.JANUARY, Arrays.asList("新年", "年货", "春节", "红包", "过年"));
        seasonalContext.put(Month.FEBRUARY, Arrays.asList("情人节", "开工", "红包", "元宵"));
        seasonalContext.put(Month.JUNE, Arrays.asList("618", "毕业", "儿童节", "高考"));
        seasonalContext.put(Month.NOVEMBER, Arrays.asList("双11", "购物节", "囤货", "预售"));
        seasonalContext.put(Month.DECEMBER, Arrays.asList("双12", "年终奖", "圣诞", "跨年"));
    }

    /**
     * Check if a transaction is a Spring Festival red packet based on date and amount
     * 检查是否为春节红包：1月28日到2月16日，且金额能整除100的转账收入
     */
    private static boolean isSpringFestivalRedPacket(Transaction transaction) {
        // 只检查收入类型的交易
        if (transaction.getType() != Transaction.Type.INCOME) {
            return false;
        }

        // 检查日期范围：1月28日到2月16日
        LocalDate date = transaction.getDate();
        if (date == null) {
            return false;
        }

        // 获取月份和日期
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        boolean inDateRange = false;
        if (month == 1 && day >= 28) {
            inDateRange = true; // 1月28日及以后
        } else if (month == 2 && day <= 16) {
            inDateRange = true; // 2月16日及以前
        }

        if (!inDateRange) {
            return false;
        }

        // 检查金额是否能整除100
        double amount = transaction.getAmount();
        if (amount <= 0 || amount % 100 != 0) {
            return false;
        }

        // 检查是否为转账收入（通过关键词或来源判断）
        String note = transaction.getNote() != null ? transaction.getNote().toLowerCase() : "";
        String source = transaction.getSource() != null ? transaction.getSource().toLowerCase() : "";
        String textToCheck = note + " " + source;

        // 转账相关关键词
        List<String> transferKeywords = Arrays.asList(
                "转账", "转入", "收到转账", "transfer", "received", "收款", "微信", "支付宝",
                "wechat", "alipay", "银行转账", "bank transfer"
        );

        for (String keyword : transferKeywords) {
            if (textToCheck.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
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
     */
    public static boolean isUsingAdvancedMode() {
        return !useSimpleMode;
    }

    public static boolean isUsingSimpleMode() {
        return useSimpleMode;
    }

    /**
     * Enhanced classify method with smart Chinese context
     */
    public static Transaction categorize(Transaction transaction) {
        // First, check if it's a Spring Festival red packet
        if (isSpringFestivalRedPacket(transaction)) {
            return new Transaction(
                    transaction.getType(),
                    "Red Packet",
                    transaction.getAmount(),
                    transaction.getDate(),
                    transaction.getNote(),
                    transaction.getSource()
            );
        }

        // Then, check the user correction history
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
            return enhancedCategorizeWithKeywords(transaction);
        } else {
            // Check if API is being used
            if (AppConfig.isUseAPI()) {
                // Call enhanced MLTransactionCategorizer for classification
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
                // No API, fall back to enhanced keyword-based classification
                return enhancedCategorizeWithKeywords(transaction);
            }
        }
    }

    /**
     * Enhanced categorize with smart pattern recognition and seasonal context
     */
    private static Transaction enhancedCategorizeWithKeywords(Transaction transaction) {
        String note = transaction.getNote() != null ? transaction.getNote().toLowerCase() : "";
        String source = transaction.getSource() != null ? transaction.getSource().toLowerCase() : "";
        String textToMatch = (note == null || note.trim().isEmpty()) ? source : note;

        if (textToMatch == null || textToMatch.trim().isEmpty()) {
            return new Transaction(
                    transaction.getType(),
                    transaction.getType() == Transaction.Type.EXPENSE ? "Other" : "Other",
                    transaction.getAmount(),
                    transaction.getDate(),
                    transaction.getNote(),
                    transaction.getSource()
            );
        }

        // Step 1: Smart pattern recognition (highest priority)
        String patternCategory = findSmartPatternMatch(textToMatch);
        if (patternCategory != null) {
            return new Transaction(
                    transaction.getType(),
                    patternCategory,
                    transaction.getAmount(),
                    transaction.getDate(),
                    transaction.getNote(),
                    transaction.getSource()
            );
        }

        // Step 2: Seasonal context analysis
        String seasonalCategory = analyzeSeasonalContext(transaction, textToMatch);
        if (seasonalCategory != null) {
            return new Transaction(
                    transaction.getType(),
                    seasonalCategory,
                    transaction.getAmount(),
                    transaction.getDate(),
                    transaction.getNote(),
                    transaction.getSource()
            );
        }

        // Step 3: Enhanced keyword matching
        Map<String, List<String>> keywordsMap =
                (transaction.getType() == Transaction.Type.EXPENSE) ? expenseKeywords : incomeKeywords;

        String category = findBestMatchingCategoryEnhanced(textToMatch, keywordsMap);

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
     * Find smart pattern matches with confidence scoring
     */
    private static String findSmartPatternMatch(String text) {
        double bestScore = 0;
        String bestCategory = null;

        for (Map.Entry<Pattern, String> entry : smartPatterns.entrySet()) {
            if (entry.getKey().matcher(text).matches()) {
                double confidence = confidenceScores.getOrDefault(entry.getKey(), 0.5);
                if (confidence > bestScore) {
                    bestScore = confidence;
                    bestCategory = entry.getValue();
                }
            }
        }

        return bestScore > 0.7 ? bestCategory : null;
    }

    /**
     * Analyze seasonal context for smarter categorization
     */
    private static String analyzeSeasonalContext(Transaction transaction, String text) {
        if (transaction.getDate() == null) return null;

        Month month = transaction.getDate().getMonth();
        List<String> monthlyKeywords = seasonalContext.get(month);

        if (monthlyKeywords != null) {
            for (String keyword : monthlyKeywords) {
                if (text.contains(keyword.toLowerCase())) {
                    // Apply seasonal logic
                    if ((month == Month.JANUARY || month == Month.FEBRUARY) &&
                            (keyword.contains("红包") || keyword.contains("年货"))) {
                        return "Red Packet";
                    }
                    if ((month == Month.JUNE || month == Month.NOVEMBER) &&
                            (keyword.contains("618") || keyword.contains("双11"))) {
                        return "Shopping";
                    }
                }
            }
        }

        return null;
    }

    /**
     * Enhanced keyword matching with better scoring
     */
    private static String findBestMatchingCategoryEnhanced(String text, Map<String, List<String>> keywordsMap) {
        Map<String, Double> scores = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : keywordsMap.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();

            double score = 0;
            for (String keyword : keywords) {
                String lowerKeyword = keyword.toLowerCase();
                if (text.contains(lowerKeyword)) {
                    // Exact match gets highest score
                    if (text.equals(lowerKeyword)) {
                        score += 5.0;
                    }
                    // Word boundary match gets high score
                    else if (text.matches(".*\\b" + Pattern.quote(lowerKeyword) + "\\b.*")) {
                        score += 3.0;
                    }
                    // Partial match gets base score
                    else {
                        score += 1.5;
                    }

                    // Bonus for longer, more specific keywords
                    if (lowerKeyword.length() > 4) {
                        score += 0.5;
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
            Transaction correctedTransaction = new Transaction(
                    originalTransaction.getType(),
                    newCategory,
                    originalTransaction.getAmount(),
                    originalTransaction.getDate(),
                    originalTransaction.getNote(),
                    originalTransaction.getSource()
            );

            MLTransactionCategorizer.learnFromTransaction(correctedTransaction);
        }

        saveUserCorrectionHistory();
    }

    /**
     * Generate a descriptive key for a transaction
     */
    private static String getDescriptiveKey(Transaction transaction) {
        if (transaction == null) return "";

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