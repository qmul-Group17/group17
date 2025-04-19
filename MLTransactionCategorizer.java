package controller;

import model.Transaction;
import java.util.*;
import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

/**
 * 基于机器学习的交易分类器
 * 结合本地分类和API分类两种方式
 */
public class MLTransactionCategorizer {

    // 存储用户交易的历史模式
    private static Map<String, Map<String, Integer>> descriptionPatterns = new HashMap<>();
    private static Map<String, Map<Double, Integer>> amountPatterns = new HashMap<>();
    private static Map<String, Map<DayOfWeek, Integer>> dayOfWeekPatterns = new HashMap<>();
    private static Map<String, Map<String, Integer>> sourcePatterns = new HashMap<>();

    // 交易类别权重
    private static Map<String, Integer> categoryFrequency = new HashMap<>();

    // 模型文件
    private static final String MODEL_FILE = "ml_model.dat";

    // 初始化模型
    static {
        loadModel();
    }



    /**
     * 加载模型数据
     */
    @SuppressWarnings("unchecked")
    private static void loadModel() {
        File modelFile = new File(MODEL_FILE);
        if (modelFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(modelFile))) {
                descriptionPatterns = (Map<String, Map<String, Integer>>) ois.readObject();
                amountPatterns = (Map<String, Map<Double, Integer>>) ois.readObject();
                dayOfWeekPatterns = (Map<String, Map<DayOfWeek, Integer>>) ois.readObject();
                sourcePatterns = (Map<String, Map<String, Integer>>) ois.readObject();
                categoryFrequency = (Map<String, Integer>) ois.readObject();

                System.out.println("已加载机器学习模型数据");
                System.out.println("交易描述模式: " + descriptionPatterns.size());
                System.out.println("金额模式: " + amountPatterns.size());
                System.out.println("星期几模式: " + dayOfWeekPatterns.size());
                System.out.println("来源模式: " + sourcePatterns.size());
                System.out.println("类别频率: " + categoryFrequency.size());
            } catch (Exception e) {
                System.err.println("加载机器学习模型失败: " + e.getMessage());
                initializeEmptyModel();
            }
        } else {
            initializeEmptyModel();
        }
    }

    /**
     * 初始化空模型
     */
    private static void initializeEmptyModel() {
        descriptionPatterns = new HashMap<>();
        amountPatterns = new HashMap<>();
        dayOfWeekPatterns = new HashMap<>();
        sourcePatterns = new HashMap<>();
        categoryFrequency = new HashMap<>();
        System.out.println("创建了新的机器学习模型");
    }

    /**
     * 保存模型数据
     */
    public static void saveModel() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(MODEL_FILE))) {
            oos.writeObject(descriptionPatterns);
            oos.writeObject(amountPatterns);
            oos.writeObject(dayOfWeekPatterns);
            oos.writeObject(sourcePatterns);
            oos.writeObject(categoryFrequency);
            System.out.println("已保存机器学习模型数据");
        } catch (Exception e) {
            System.err.println("保存机器学习模型失败: " + e.getMessage());
        }
    }

    /**
     * 学习交易模式
     * @param transaction 已分类的交易
     */
    public static void learnFromTransaction(Transaction transaction) {
        String category = transaction.getCategory();
        String description = transaction.getNote().toLowerCase();
        Double amount = transaction.getAmount();
        LocalDate date = transaction.getDate();
        String source = transaction.getSource().toLowerCase();

        // 更新类别频率
        categoryFrequency.put(category, categoryFrequency.getOrDefault(category, 0) + 1);

        // 更新描述模式
        Map<String, Integer> descriptionMap = descriptionPatterns.getOrDefault(description, new HashMap<>());
        descriptionMap.put(category, descriptionMap.getOrDefault(category, 0) + 1);
        descriptionPatterns.put(description, descriptionMap);

        // 更新金额模式
        Map<Double, Integer> amountMap = amountPatterns.getOrDefault(category, new HashMap<>());
        amountMap.put(amount, amountMap.getOrDefault(amount, 0) + 1);
        amountPatterns.put(category, amountMap);

        // 更新星期几模式
        if (date != null) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            Map<DayOfWeek, Integer> dayMap = dayOfWeekPatterns.getOrDefault(category, new HashMap<>());
            dayMap.put(dayOfWeek, dayMap.getOrDefault(dayOfWeek, 0) + 1);
            dayOfWeekPatterns.put(category, dayMap);
        }

        // 更新来源模式
        Map<String, Integer> sourceMap = sourcePatterns.getOrDefault(category, new HashMap<>());
        sourceMap.put(source, sourceMap.getOrDefault(source, 0) + 1);
        sourcePatterns.put(category, sourceMap);

        // 保存更新后的模型
        saveModel();
    }

    /**
     * 对交易进行智能分类
     * @param transaction 需要分类的交易
     * @return 预测的类别
     */
    public static String predictCategory(Transaction transaction) {
        // 首先尝试调用API进行分类
        String apiCategory = categorizeViaAPI(transaction);
        if (apiCategory != null) {
            return apiCategory;
        }

        // 如果API调用失败，回退到本地模型
        System.out.println("API分类失败，使用本地模型进行分类");

        // 如果模型为空，返回默认分类
        if (categoryFrequency.isEmpty()) {
            return transaction.getType() == Transaction.Type.EXPENSE ? "其他" : "其他收入";
        }

        String description = transaction.getNote().toLowerCase();
        Double amount = transaction.getAmount();
        DayOfWeek dayOfWeek = transaction.getDate() != null ? transaction.getDate().getDayOfWeek() : null;
        String source = transaction.getSource().toLowerCase();

        Map<String, Double> scores = new HashMap<>();

        // 初始化所有类别的分数为它们的基础频率
        for (Map.Entry<String, Integer> entry : categoryFrequency.entrySet()) {
            String category = entry.getKey();
            int frequency = entry.getValue();
            // 基础得分为该类别在所有交易中的比例
            double baseScore = (double) frequency / categoryFrequency.values().stream().mapToInt(Integer::intValue).sum();
            scores.put(category, baseScore);
        }

        // 增加描述匹配的分数
        if (descriptionPatterns.containsKey(description)) {
            Map<String, Integer> categoryMatches = descriptionPatterns.get(description);
            for (Map.Entry<String, Integer> entry : categoryMatches.entrySet()) {
                String category = entry.getKey();
                int count = entry.getValue();
                // 描述完全匹配是一个很强的信号
                scores.put(category, scores.getOrDefault(category, 0.0) + count * 3.0);
            }
        } else {
            // 关键词部分匹配
            for (Map.Entry<String, Map<String, Integer>> entry : descriptionPatterns.entrySet()) {
                String knownDesc = entry.getKey();
                if (description.contains(knownDesc) || knownDesc.contains(description)) {
                    Map<String, Integer> categoryMatches = entry.getValue();
                    for (Map.Entry<String, Integer> catEntry : categoryMatches.entrySet()) {
                        String category = catEntry.getKey();
                        int count = catEntry.getValue();
                        // 部分匹配给较低的分数
                        scores.put(category, scores.getOrDefault(category, 0.0) + count * 0.5);
                    }
                }
            }
        }

        // 增加金额相似性的分数
        for (Map.Entry<String, Map<Double, Integer>> entry : amountPatterns.entrySet()) {
            String category = entry.getKey();
            Map<Double, Integer> amountMap = entry.getValue();

            // 检查相似金额的模式
            for (Map.Entry<Double, Integer> amountEntry : amountMap.entrySet()) {
                double knownAmount = amountEntry.getKey();
                int count = amountEntry.getValue();

                // 计算金额的相似度（金额越接近，分数越高）
                double similarity = 1.0 / (1.0 + Math.abs(amount - knownAmount) / knownAmount);
                scores.put(category, scores.getOrDefault(category, 0.0) + similarity * count * 0.5);
            }
        }

        // 增加星期几模式的分数
        if (dayOfWeek != null) {
            for (Map.Entry<String, Map<DayOfWeek, Integer>> entry : dayOfWeekPatterns.entrySet()) {
                String category = entry.getKey();
                Map<DayOfWeek, Integer> dayMap = entry.getValue();

                if (dayMap.containsKey(dayOfWeek)) {
                    int count = dayMap.get(dayOfWeek);
                    scores.put(category, scores.getOrDefault(category, 0.0) + count * 0.2);
                }
            }
        }

        // 增加来源模式的分数
        for (Map.Entry<String, Map<String, Integer>> entry : sourcePatterns.entrySet()) {
            String category = entry.getKey();
            Map<String, Integer> sourceMap = entry.getValue();

            if (sourceMap.containsKey(source)) {
                int count = sourceMap.get(source);
                scores.put(category, scores.getOrDefault(category, 0.0) + count * 0.8);
            }
        }

        // 找出得分最高的类别
        String bestCategory = null;
        double bestScore = 0;

        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }

        // 如果没有找到合适的类别，返回默认类别
        if (bestCategory == null) {
            return transaction.getType() == Transaction.Type.EXPENSE ? "其他" : "其他收入";
        }

        return bestCategory;
    }

    /**
     * 通过DeepSeek API对交易进行分类
     * @param transaction 需要分类的交易
     * @return 分类结果，如果API调用失败则返回null
     */
    private static String categorizeViaAPI(Transaction transaction) {
        try {
            // 检查API配置
            String apiUrl = AppConfig.getApiUrl();
            String apiKey = AppConfig.getApiKey();

            if (!AppConfig.isUseAPI() || apiUrl.isEmpty() || apiKey.isEmpty()) {
                return null;
            }

            // 创建交易描述
            String description = transaction.getNote();
            if (description == null || description.isEmpty()) {
                description = transaction.getSource();
            }

            double amount = transaction.getAmount();
            String date = transaction.getDate() != null ? transaction.getDate().toString() : "";
            String type = transaction.getType().toString();

            // 创建API请求
            StringBuilder prompt = new StringBuilder();
            prompt.append("分析以下交易信息，并将其分类到最合适的类别中:\n\n");
            prompt.append("交易描述: ").append(description).append("\n");
            prompt.append("金额: ").append(amount).append("\n");
            prompt.append("日期: ").append(date).append("\n");
            prompt.append("类型: ").append(type).append("\n\n");

            if (transaction.getType() == Transaction.Type.EXPENSE) {
                prompt.append("Please select the most appropriate expense category from the options below and return the English category name: Food, Transport, Shopping, Health, Travel, Beauty, Entertainment, Transfer, Housing, Social, Education, Communication, RedPacket, Investment, Lending, Repayment, Parenting, Pet, Other");
            } else {
                prompt.append("Please select the most appropriate income category from the options below and return the English category name: TransferIn, Salary, Investment, RedPacketIn, Borrowing, Receipt, Other");
            }
            prompt.append("\n\nReturn only the category name without any additional text.”);

            // 构建请求体
            String requestBody = "{"
                    + "\"model\": \"deepseek-chat\","
                    + "\"messages\": ["
                    + "  {\"role\": \"user\", \"content\": \"" + escapeJson(prompt.toString()) + "\"}"
                    + "],"
                    + "\"temperature\": 0.0,"
                    + "\"max_tokens\": 10"
                    + "}";

            // 发送API请求
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

            // 读取响应
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // 解析DeepSeek API响应
                    String apiResponse = response.toString();
                    String category = parseDeepSeekResponse(apiResponse);

                    System.out.println("API分类结果: " + category);
                    return category;
                }
            } else {
                System.err.println("API调用失败，HTTP错误码: " + responseCode);
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    System.err.println("错误响应: " + response.toString());
                }
                return null;
            }
        } catch (Exception e) {
            System.err.println("API分类过程中发生异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解析DeepSeek API响应
     */
    private static String parseDeepSeekResponse(String apiResponse) {
        try {
            // 查找"content"字段
            int contentStart = apiResponse.indexOf("\"content\":");
            if (contentStart == -1) {
                throw new RuntimeException("API响应中未找到content字段");
            }

            contentStart = apiResponse.indexOf("\"", contentStart + 10) + 1;
            int contentEnd = apiResponse.indexOf("\"", contentStart);

            String content = apiResponse.substring(contentStart, contentEnd);

            // 清理响应，只保留类别名称
            content = content.trim();

            Map<String, String> zhToEnMap = new HashMap<String, String>() {{
                // 支出分类映射
                put("餐饮", "Food");          put("饮食", "Food");
                put("购物", "Shopping");      put("消费", "Shopping");
                put("交通", "Transport");     put("出行", "Transport");
                put("健康", "Health");        put("医疗", "Health");
                put("旅行", "Travel");        put("旅游", "Travel");
                put("娱乐", "Entertainment"); put("游戏", "Entertainment");
                put("住房", "Housing");       put("房租", "Housing");
                put("社交", "Social");        put("聚会", "Social");
                put("教育", "Education");     put("学习", "Education");
                put("通讯", "Communication"); put("电话", "Communication");
                put("投资", "Investment");    put("理财", "Investment");
                put("还款", "Repayment");     put("贷款", "Repayment");
                put("育儿", "Parenting");     put("儿童", "Parenting");
                put("宠物", "Pet");           put("养宠", "Pet");

                // 收入分类映射
                put("转账", "TransferIn");    put("转入", "TransferIn");
                put("工资", "Salary");        put("薪水", "Salary");
                put("红包", "RedPacketIn");   put("礼金", "RedPacketIn");
                put("借款", "Borrowing");    put("贷款", "Borrowing");
                put("收款", "Receipt");      put("到账", "Receipt");

                // 通用分类
                put("其他", "Other");        put("其它", "Other");
            }};

            // 优先处理中文映射（不区分大小写）
            String lowerContent = content.toLowerCase();
            for (Map.Entry<String, String> entry : zhToEnMap.entrySet()) {
                if (lowerContent.contains(entry.getKey().toLowerCase())) {
                    return entry.getValue();
                }
            }


            List<String> validExpenseCategories = Arrays.asList(
                    "Food", "Transport", "Shopping", "Health", "Travel", "Beauty", "Entertainment", "Transfer",
                    "Housing", "Social", "Education", "Communication", "RedPacket", "Investment",
                    "Lending", "Repayment", "Parenting", "Pet", "Other"
            );

            List<String> validIncomeCategories = Arrays.asList(
                    "TransferIn", "Salary", "Investment", "RedPacketIn", "Borrowing", "Receipt", "Other"
            );

            // 如果获取到的类别名有效，则返回
            if (validExpenseCategories.contains(content) || validIncomeCategories.contains(content)) {
                return content;
            }

            // 否则尝试模糊匹配
            for (String category : validExpenseCategories) {
                if (content.contains(category)) {
                    return category;
                }
            }

            for (String category : validIncomeCategories) {
                if (content.contains(category)) {
                    return category;
                }
            }

            // 如果无法匹配，返回默认值
            return "其他";
        } catch (Exception e) {
            System.err.println("解析API响应失败: " + e.getMessage());
            return "其他";
        }
    }

    /**
     * 转义JSON字符串
     */
    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}