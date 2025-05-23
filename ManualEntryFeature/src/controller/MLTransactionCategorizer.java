package controller;

import model.Transaction;
import java.util.*;
import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

/**
 * Machine learning-based transaction classifier
 * Combine local classification and API classification
 */
public class MLTransactionCategorizer {

    // Stores the historical patterns of user transactions
    private static Map<String, Map<String, Integer>> descriptionPatterns = new HashMap<>();
    private static Map<String, Map<Double, Integer>> amountPatterns = new HashMap<>();
    private static Map<String, Map<DayOfWeek, Integer>> dayOfWeekPatterns = new HashMap<>();
    private static Map<String, Map<String, Integer>> sourcePatterns = new HashMap<>();

    // Transaction category weights
    private static Map<String, Integer> categoryFrequency = new HashMap<>();

    // Model files
    private static final String MODEL_FILE = "ml_model.dat";

    // Initialize the model
    static {
        loadModel();
    }

    private static final Map<String, String> chineseToEnglishCategory = new HashMap<>();
    private static final Map<String, String> englishToChineseCategory = new HashMap<>();

    static {
        // Expense category mapping
        chineseToEnglishCategory.put("餐饮", "Food");
        chineseToEnglishCategory.put("交通", "Transport");
        chineseToEnglishCategory.put("购物", "Shopping");
        chineseToEnglishCategory.put("健康", "Health");
        chineseToEnglishCategory.put("旅游", "Travel");
        chineseToEnglishCategory.put("美妆", "Beauty");
        chineseToEnglishCategory.put("娱乐", "Entertainment");
        chineseToEnglishCategory.put("转账", "Transfer");
        chineseToEnglishCategory.put("住房", "Housing");
        chineseToEnglishCategory.put("人情社交", "Social");
        chineseToEnglishCategory.put("教育", "Education");
        chineseToEnglishCategory.put("通讯", "Communication");
        chineseToEnglishCategory.put("红包", "RedPacket");
        chineseToEnglishCategory.put("投资", "Investment");
        chineseToEnglishCategory.put("借出", "Lending");
        chineseToEnglishCategory.put("还款", "Repayment");
        chineseToEnglishCategory.put("亲子", "Parenting");
        chineseToEnglishCategory.put("宠物", "Pet");
        chineseToEnglishCategory.put("其他", "Other");

        // Income Category Mapping
        chineseToEnglishCategory.put("收转账", "TransferIn");
        chineseToEnglishCategory.put("薪资", "Salary");
        chineseToEnglishCategory.put("理财", "Investment");
        chineseToEnglishCategory.put("收红包", "RedPacketIn");
        chineseToEnglishCategory.put("借入", "Borrowing");
        chineseToEnglishCategory.put("收款", "Receipt");
        chineseToEnglishCategory.put("其他收入", "Other");

        // Create a reverse map at the same time
        for (Map.Entry<String, String> entry : chineseToEnglishCategory.entrySet()) {
            englishToChineseCategory.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Convert the category to English (if Chinese)
     */
    private static String toCategoryEnglish(String category) {
        return chineseToEnglishCategory.getOrDefault(category, category);
    }


    /**
     * Convert categories to Chinese (if English)
     */
    private static String toCategoryChinese(String category) {
        return englishToChineseCategory.getOrDefault(category, category);
    }

    /**
     * Load the model data
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

                System.out.println("Machine learning model data is loaded");
                System.out.println("Transaction Description Mode: " + descriptionPatterns.size());
                System.out.println("Amount Pattern: " + amountPatterns.size());
                System.out.println("Day of the week mode: " + dayOfWeekPatterns.size());
                System.out.println("Source Mode: " + sourcePatterns.size());
                System.out.println("Category Frequency: " + categoryFrequency.size());
            } catch (Exception e) {
                System.err.println("Failed to load machine learning model: " + e.getMessage());
                initializeEmptyModel();
            }
        } else {
            initializeEmptyModel();
        }
    }

    /**
     * Initialize the empty model
     */
    private static void initializeEmptyModel() {
        descriptionPatterns = new HashMap<>();
        amountPatterns = new HashMap<>();
        dayOfWeekPatterns = new HashMap<>();
        sourcePatterns = new HashMap<>();
        categoryFrequency = new HashMap<>();
        System.out.println("A new machine learning model was created");
    }

    /**
     * Save the model data
     */
    public static void saveModel() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(MODEL_FILE))) {
            oos.writeObject(descriptionPatterns);
            oos.writeObject(amountPatterns);
            oos.writeObject(dayOfWeekPatterns);
            oos.writeObject(sourcePatterns);
            oos.writeObject(categoryFrequency);
            System.out.println("Machine learning model data has been saved");
        } catch (Exception e) {
            System.err.println("Saving machine learning model failed: " + e.getMessage());
        }
    }

    /**
     * Learn trading patterns
     * @param transaction Categorized transactions
     */
    public static void learnFromTransaction(Transaction transaction) {
        String category = transaction.getCategory();
        String description = transaction.getNote().toLowerCase();
        Double amount = transaction.getAmount();
        LocalDate date = transaction.getDate();
        String source = transaction.getSource().toLowerCase();

        // Update category frequency
        categoryFrequency.put(category, categoryFrequency.getOrDefault(category, 0) + 1);

        // Updated description mode
        Map<String, Integer> descriptionMap = descriptionPatterns.getOrDefault(description, new HashMap<>());
        descriptionMap.put(category, descriptionMap.getOrDefault(category, 0) + 1);
        descriptionPatterns.put(description, descriptionMap);

        // Update the amount model
        Map<Double, Integer> amountMap = amountPatterns.getOrDefault(category, new HashMap<>());
        amountMap.put(amount, amountMap.getOrDefault(amount, 0) + 1);
        amountPatterns.put(category, amountMap);

        // Updated Day of the Week mode
        if (date != null) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            Map<DayOfWeek, Integer> dayMap = dayOfWeekPatterns.getOrDefault(category, new HashMap<>());
            dayMap.put(dayOfWeek, dayMap.getOrDefault(dayOfWeek, 0) + 1);
            dayOfWeekPatterns.put(category, dayMap);
        }

        // Update the source mode
        Map<String, Integer> sourceMap = sourcePatterns.getOrDefault(category, new HashMap<>());
        sourceMap.put(source, sourceMap.getOrDefault(source, 0) + 1);
        sourcePatterns.put(category, sourceMap);

        // Save the updated model
        saveModel();
    }

    /**
     * Intelligently classify transactions
     * @param transaction Transactions that need to be categorized
     * @return The category of the prediction
     */
    public static String predictCategory(Transaction transaction) {
        // First, try to call the API to classify it
        String apiCategory = categorizeViaAPI(transaction);
        if (apiCategory != null) {
            return apiCategory;
        }

        // If the API call fails, roll back to the local model
        System.out.println("API classification fails, and the local model is used for classification");

        // If the model is empty, the default classification is returned
        if (categoryFrequency.isEmpty()) {
            return transaction.getType() == Transaction.Type.EXPENSE ? "Other" : "Other income";
        }

        String description = transaction.getNote().toLowerCase();
        Double amount = transaction.getAmount();
        DayOfWeek dayOfWeek = transaction.getDate() != null ? transaction.getDate().getDayOfWeek() : null;
        String source = transaction.getSource().toLowerCase();

        Map<String, Double> scores = new HashMap<>();

        // Initialize the scores for all categories as their base frequency
        for (Map.Entry<String, Integer> entry : categoryFrequency.entrySet()) {
            String category = entry.getKey();
            int frequency = entry.getValue();
            // The base score is the proportion of the category among all transactions
            double baseScore = (double) frequency / categoryFrequency.values().stream().mapToInt(Integer::intValue).sum();
            scores.put(category, baseScore);
        }

        // Increase the score for description matching
        if (descriptionPatterns.containsKey(description)) {
            Map<String, Integer> categoryMatches = descriptionPatterns.get(description);
            for (Map.Entry<String, Integer> entry : categoryMatches.entrySet()) {
                String category = entry.getKey();
                int count = entry.getValue();
                // Describing an exact match is a strong signal
                scores.put(category, scores.getOrDefault(category, 0.0) + count * 3.0);
            }
        } else {
            // Keyword partial match
            for (Map.Entry<String, Map<String, Integer>> entry : descriptionPatterns.entrySet()) {
                String knownDesc = entry.getKey();
                if (description.contains(knownDesc) || knownDesc.contains(description)) {
                    Map<String, Integer> categoryMatches = entry.getValue();
                    for (Map.Entry<String, Integer> catEntry : categoryMatches.entrySet()) {
                        String category = catEntry.getKey();
                        int count = catEntry.getValue();
                        // A partial match is given to a lower score
                        scores.put(category, scores.getOrDefault(category, 0.0) + count * 0.5);
                    }
                }
            }
        }

        // Increase the score for the similarity of the amounts
        for (Map.Entry<String, Map<Double, Integer>> entry : amountPatterns.entrySet()) {
            String category = entry.getKey();
            Map<Double, Integer> amountMap = entry.getValue();

            // Check patterns for similar amounts
            for (Map.Entry<Double, Integer> amountEntry : amountMap.entrySet()) {
                double knownAmount = amountEntry.getKey();
                int count = amountEntry.getValue();

                // Calculate the similarity of the amounts (the closer the amounts, the higher the score)
                double similarity = 1.0 / (1.0 + Math.abs(amount - knownAmount) / knownAmount);
                scores.put(category, scores.getOrDefault(category, 0.0) + similarity * count * 0.5);
            }
        }

        // Increase the score for the Day of the Week pattern
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

        // Increase the score of the source pattern
        for (Map.Entry<String, Map<String, Integer>> entry : sourcePatterns.entrySet()) {
            String category = entry.getKey();
            Map<String, Integer> sourceMap = entry.getValue();

            if (sourceMap.containsKey(source)) {
                int count = sourceMap.get(source);
                scores.put(category, scores.getOrDefault(category, 0.0) + count * 0.8);
            }
        }

        // Find out which category has the highest score
        String bestCategory = null;
        double bestScore = 0;

        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }

        // If no suitable category is found, return to the default category
        if (bestCategory == null) {
            return transaction.getType() == Transaction.Type.EXPENSE ? "其他" : "其他收入";
        }

        return bestCategory;
    }

    /**
     * Categorize transactions through the DeepSeek API
     * @param transaction Transactions that need to be categorized
     * @return Classify the results and return null if the API call fails
     */
    private static String categorizeViaAPI(Transaction transaction) {
        try {
            // Check the API configuration
            String apiUrl = AppConfig.getApiUrl();
            String apiKey = AppConfig.getApiKey();

            if (!AppConfig.isUseAPI() || apiUrl.isEmpty() || apiKey.isEmpty()) {
                return null;
            }

            // Create a transaction description
            String description = transaction.getNote();
            if (description == null || description.isEmpty()) {
                description = transaction.getSource();
            }

            double amount = transaction.getAmount();
            String date = transaction.getDate() != null ? transaction.getDate().toString() : "";
            String type = transaction.getType().toString();

            // Create an API request
            StringBuilder prompt = new StringBuilder();
            prompt.append("Analyze the following transaction information and classify it into the most appropriate category:\n\n");
            prompt.append("Transaction Description: ").append(description).append("\n");
            prompt.append("Amount: ").append(amount).append("\n");
            prompt.append("Date: ").append(date).append("\n");
            prompt.append("Type: ").append(type).append("\n\n");

            if (transaction.getType() == Transaction.Type.EXPENSE) {
                prompt.append("Please select the most appropriate expense category from the following and return the English category name: Food, Transport, Shopping, Health, Travel, Beauty, Entertainment, Transfer, Housing, Social, Education, Communication, RedPacket, Investment, Lending, Repayment, Parenting, Pet, Other");
            } else {
                prompt.append("Please select the most appropriate expense category from the following and return the English category name: TransferIn, Salary, Investment, RedPacketIn, Borrowing, Receipt, Other");
            }
            prompt.append("\n\nJust reply with the category name and nothing else.");

            // Build the request body
            String requestBody = "{"
                    + "\"model\": \"deepseek-chat\","
                    + "\"messages\": ["
                    + "  {\"role\": \"user\", \"content\": \"" + escapeJson(prompt.toString()) + "\"}"
                    + "],"
                    + "\"temperature\": 0.0,"
                    + "\"max_tokens\": 10"
                    + "}";

            // Send an API request
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

            // Read the response
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    // Parse the DeepSeek API response
                    String apiResponse = response.toString();
                    String category = parseDeepSeekResponse(apiResponse);

                    System.out.println("API Classification Results: " + category);
                    return category;
                }
            } else {
                System.err.println("The API call fails and the HTTP error code is as follows: " + responseCode);
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    System.err.println("Error Response: " + response.toString());
                }
                return null;
            }
        } catch (Exception e) {
            System.err.println("An exception occurred during API classification: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Parse the DeepSeek API response
     */
    private static String parseDeepSeekResponse(String apiResponse) {
        try {
            // Look for the "content" field
            int contentStart = apiResponse.indexOf("\"content\":");
            if (contentStart == -1) {
                throw new RuntimeException("The content field was not found in the API response");
            }

            contentStart = apiResponse.indexOf("\"", contentStart + 10) + 1;
            int contentEnd = apiResponse.indexOf("\"", contentStart);

            String content = apiResponse.substring(contentStart, contentEnd);

            // 清理响应，只保留类别名称
            content = content.trim();
            System.out.println("Original API response content: " + content);

            List<String> validExpenseCategories = Arrays.asList(
                    "Food", "Transport", "Shopping", "Health", "Travel", "Beauty", "Entertainment", "Transfer",
                    "Housing", "Social", "Education", "Communication", "RedPacket", "Investment",
                    "Lending", "Repayment", "Parenting", "Pet", "Other"
            );

            List<String> validIncomeCategories = Arrays.asList(
                    "TransferIn", "Salary", "Investment", "RedPacketIn", "Borrowing", "Receipt", "Other"
            );

            // First, try to match the English category directly
            if (validExpenseCategories.contains(content) || validIncomeCategories.contains(content)) {
                // If it is directly in English, there is no need to convert
                System.out.println("The API returns a valid English category");
                return content;
            }

            // If it is not a valid English category, try converting Chinese to English
            String englishCategory = toCategoryEnglish(content);

            // Check if it is different from the original after conversion (description of the conversion)
            if (!englishCategory.equals(content)) {
                System.out.println("The Chinese category'" + content + "'has been converted to the English category'" + englishCategory + "'");
            }

            // Check whether the converted English category is valid
            if (validExpenseCategories.contains(englishCategory) || validIncomeCategories.contains(englishCategory)) {
                return englishCategory;
            }

            // If it's still not a valid category after conversion, try fuzzy matching
            System.out.println("Try fuzzy matching categories: " + englishCategory);
            for (String category : validExpenseCategories) {
                if (englishCategory.contains(category) || category.contains(englishCategory)) {
                    System.out.println("Fuzzy Match to Expense Category: " + category);
                    return category;
                }
            }

            for (String category : validIncomeCategories) {
                if (englishCategory.contains(category) || category.contains(englishCategory)) {
                    System.out.println("Fuzzy Match to Income Category: " + category);
                    return category;
                }
            }

            // If it can't be matched, the default value is returned
            System.out.println("Unable to match a valid category, returns the default value: Other");
            return "Other";
        } catch (Exception e) {
            System.err.println("Parsing API response failed: " + e.getMessage());
            e.printStackTrace();
            return "Other";
        }
    }

    /**
     * Escape JSON strings
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