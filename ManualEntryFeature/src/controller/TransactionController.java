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


    public List<Transaction> getAllTransactions() {
        return transactions;
    }

    private void saveTransactions() {
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