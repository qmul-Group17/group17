package controller;

import model.Transaction;
import model.Transaction.Type;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CSVImporter {
    public static List<Transaction> importFromCSV(String filePath) {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 6) continue;

                Type type = Type.valueOf(parts[0].trim().toUpperCase());
                String category = parts[1].trim();
                double amount = Double.parseDouble(parts[2].trim());
                LocalDate date = LocalDate.parse(parts[3].trim());
                String note = parts[4].trim();
                String source = parts[5].trim();

                transactions.add(new Transaction(type, category, amount, date, note, source));
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to import CSV: " + e.getMessage());
        }

        return transactions;
    }
}