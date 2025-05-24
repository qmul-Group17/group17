package controller;

import model.Transaction;
import model.User; // New import
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

    private String DATA_FILE = "transactions.json"; // default value for backward compatibility
    private User currentUser; // Store the logged-in user

    // Overloaded constructor with user
    public TransactionController(User user) {
        this.currentUser = user;
        this.DATA_FILE = "PersonalFinanceTracker/transactions_" + user.getUsername() + ".json";
    }

    // Default constructor kept for compatibility (not used in login-based version)
    public TransactionController() {}

    // A custom TypeAdapter is used to handle LocalDate
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
        transactions.sort((o1,o2) -> o2.getDate().compareTo(o1.getDate()));
        saveTransactions(); // Auto-save
    }

    public void importTransactions(List<Transaction> importedTransactions) {
        transactions.addAll(importedTransactions);
        saveTransactions(); // Auto-save
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

    // Add and automatically categorize transactions
    public void addAndCategorizeTransaction(Transaction transaction) {
        // Use classifiers to automatically classify
        Transaction categorizedTransaction = TransactionCategorizer.categorize(transaction);
        transactions.add(categorizedTransaction);
        saveTransactions();
        notifyListeners();
    }

    // Delete Transaction
    public void deleteTransaction(int index) {
        if (index >= 0 && index < transactions.size()) {
            transactions.remove(index);
            saveTransactions();
            notifyListeners();
        }
    }

    // Update Transaction
    public void updateTransaction(int index, Transaction newTransaction) {
        if (index >= 0 && index < transactions.size()) {
            transactions.set(index, newTransaction);
            saveTransactions();
            notifyListeners();
        }
    }

    // Update the transaction category
    public void updateCategory(int index, String newCategory) {
        if (index >= 0 && index < transactions.size()) {
            Transaction oldTransaction = transactions.get(index);

            // Record the user's remediation for machine learning
            TransactionCategorizer.recordUserCorrection(oldTransaction, newCategory);

            // Create a new transaction with updated category
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

    // Reclassify all transactions
    public void recategorizeAll() {
        List<Transaction> recategorized = TransactionCategorizer.categorizeAll(new ArrayList<>(transactions));
        transactions.clear();
        transactions.addAll(recategorized);
        saveTransactions();
        notifyListeners();
    }

    // Add listener-related code
    private List<TransactionChangeListener> listeners = new ArrayList<>();

    /**
     * Add a transaction change listener
     */
    public void addChangeListener(TransactionChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Notify all listeners that the transaction has changed
     */
    private void notifyListeners() {
        for (TransactionChangeListener listener : listeners) {
            listener.onTransactionsChanged();
        }
    }

    /**
     * Import a CSV file
     */
    public void importFromCSV(String filePath) {
        // Use CSVImporter to import data and automatically classify it
        CSVImporter importer = new CSVImporter();
        List<Transaction> importedTransactions = importer.importTransactions(filePath);

        // Automatically classify imported transactions
        List<Transaction> categorizedTransactions = TransactionCategorizer.categorizeAll(importedTransactions);

        // Add to the list of existing transactions
        transactions.addAll(categorizedTransactions);
        saveTransactions();
        notifyListeners();
    }

    /**
     * Transaction change listener interface
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
            transactions.sort((o1,o2) -> {
                return o2.getDate().compareTo(o1.getDate());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
