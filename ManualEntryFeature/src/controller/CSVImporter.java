package controller;

import model.Transaction;
import model.Transaction.Type;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CSVImporter {

    // Supported date formats
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    public static List<Transaction> importFromCSV(String filePath) {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;

            // Read the CSV file
            while ((line = br.readLine()) != null) {
                // Skip the header
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                try {
                    // Try using a simple comma separation first
                    String[] parts = line.split(",");

                    // If the split results are not enough, try using more complex CSV parsing
                    if (parts.length < 4) {
                        parts = parseCSVLine(line);
                    }

                    if (parts.length < 4) {
                        System.err.println("Skip invalid lines: " + line);
                        continue;
                    }

                    // Resolution type
                    Type type = parseType(parts[0]);

                    // Parse amount (keep the original classification first, and then the AI will reclassify it later)
                    String category = parts.length > 1 ? parts[1].trim() : "Pending";

                    // Parse the amount
                    double amount = parseAmount(parts[2]);

                    // Parse date
                    LocalDate date = parseDate(parts[3]);

                    // Parse notes and sources
                    String note = parts.length > 4 ? parts[4].trim() : "";
                    String source = parts.length > 5 ? parts[5].trim() : "CSV import";

                    // Create a transaction record - note that there is no direct classification here, keep the original classification or use "Pending"
                    // In the future, the TransactionCategorizer will be used in the MainFrame for classification
                    // Transactions are not categorized immediately, let the controller handle the categorization
                    Transaction transaction = new Transaction(type, "To be classified", amount, date, note, source);
                    transactions.add(transaction);

                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line + ", error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to import CSV: " + e.getMessage());
        }

        return transactions;
    }

    /**
     * Import transactions from a CSV file
     * @param filePath CSV file path
     * @return A list of imported transactions
     */
    public List<Transaction> importTransactions(String filePath) {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                // Skip the header
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Parse CSV rows
                String[] values = line.split(",");
                if (values.length >= 6) {
                    try {
                        // Create a trading object
                        Transaction.Type type = Transaction.Type.valueOf(values[0].trim());
                        String category = values[1].trim();
                        double amount = Double.parseDouble(values[2].trim());
                        LocalDate date = LocalDate.parse(values[3].trim());
                        String note = values[4].trim();
                        String source = values[5].trim();

                        Transaction transaction = new Transaction(type, category, amount, date, note, source);
                        transactions.add(transaction);
                    } catch (Exception e) {
                        System.err.println("Parsing CSV rows failed: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read CSV file: " + e.getMessage());
        }

        return transactions;
    }

    /**
     * Parsing CSV lines (handling commas inside quotation marks)
     */
    private static String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentValue = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }

        values.add(currentValue.toString().trim());
        return values.toArray(new String[0]);
    }

    /**
     * Parse the transaction type
     */
    private static Type parseType(String value) {
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("INCOME") ||
                normalized.contains("收入") ||
                normalized.contains("入账") ||
                normalized.equals("IN")) {
            return Type.INCOME;
        } else {
            return Type.EXPENSE;
        }
    }

    /**
     * Parse the amount
     */
    private static double parseAmount(String value) {
        // Remove currency symbols and commas
        String normalized = value.replaceAll("[¥$,]", "").trim();
        return Double.parseDouble(normalized);
    }

    /**
     * Try parsing the date in a different format
     */
    private static LocalDate parseDate(String value) {
        String normalized = value.trim();

        // Try parsing directly first
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ignored) {
            // If direct parsing fails, try a different format
        }

        // Experiment with different date formats
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // Go ahead and try the next format
            }
        }

        // If all formatting fails, the current date is used
        System.err.println("Unable to resolve date: " + value + ", use the current date");
        return LocalDate.now();
    }
}
