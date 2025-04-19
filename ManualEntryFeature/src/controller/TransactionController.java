package controller;

import model.Transaction;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransactionController {
    private final List<Transaction> transactions = new ArrayList<>();
    private final String DATA_FILE = "transactions.json";

    // 自定义的 TypeAdapter 用于处理 LocalDate
    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;

        @Override
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(formatter));
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return LocalDate.parse(json.getAsString(), formatter);
        }
    }

    public void addTransaction(Transaction t) {
        transactions.add(t);
        saveTransactions(); // 自动保存
    }

    public void importTransactions(List<Transaction> importedTransactions) {
        transactions.addAll(importedTransactions);
        saveTransactions(); // 自动保存
    }

    public List<Transaction> getAllTransactions() {
        return transactions;
    }

    public void saveTransactions() {
        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                    .setPrettyPrinting()
                    .create();
            String json = gson.toJson(transactions);
            try (FileWriter writer = new FileWriter(DATA_FILE)) {
                writer.write(json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 添加并自动分类交易
    public void addAndCategorizeTransaction(Transaction transaction) {
        // 使用分类器自动分类
        Transaction categorizedTransaction = TransactionCategorizer.categorize(transaction);
        transactions.add(categorizedTransaction);
        saveTransactions();
        notifyListeners();
    }

    // 更新交易分类
    public void updateCategory(int index, String newCategory) {
        if (index >= 0 && index < transactions.size()) {
            Transaction oldTransaction = transactions.get(index);

            // 记录用户的修正用于机器学习
            TransactionCategorizer.recordUserCorrection(oldTransaction, newCategory);

            // 创建新的交易对象，只更新类别
            Transaction updatedTransaction = new Transaction(
                    oldTransaction.getType(),
                    newCategory,
                    oldTransaction.getAmount(),
                    oldTransaction.getDate(),
                    oldTransaction.getNote(),
                    oldTransaction.getSource()
            );

            transactions.set(index, updatedTransaction);
            saveTransactions();
            notifyListeners();
        }
    }

    // 对所有交易重新分类
    public void recategorizeAll() {
        List<Transaction> recategorized = TransactionCategorizer.categorizeAll(new ArrayList<>(transactions));
        transactions.clear();
        transactions.addAll(recategorized);
        saveTransactions();
        notifyListeners();
    }

    // 添加监听器相关代码
    private List<TransactionChangeListener> listeners = new ArrayList<>();

    /**
     * 添加交易变更监听器
     */
    public void addChangeListener(TransactionChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * 通知所有监听器交易已变更
     */
    private void notifyListeners() {
        for (TransactionChangeListener listener : listeners) {
            listener.onTransactionsChanged();
        }
    }

    /**
     * 导入CSV文件
     */
    public void importFromCSV(String filePath) {
        // 使用CSVImporter导入数据并自动分类
        CSVImporter importer = new CSVImporter();
        List<Transaction> importedTransactions = importer.importTransactions(filePath);

        // 对导入的交易进行自动分类
        List<Transaction> categorizedTransactions = TransactionCategorizer.categorizeAll(importedTransactions);

        // 添加到现有交易列表
        transactions.addAll(categorizedTransactions);
        saveTransactions();
        notifyListeners();
    }

    /**
     * 交易变更监听器接口
     */
    public interface TransactionChangeListener {
        void onTransactionsChanged();
    }

    public void loadTransactions() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(DATA_FILE)) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                    .create();
            Type listType = new TypeToken<ArrayList<Transaction>>() {}.getType();
            transactions.clear();
            transactions.addAll(gson.fromJson(reader, listType));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}