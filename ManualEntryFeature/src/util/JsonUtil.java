package util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.lang.reflect.Type;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static <T> T readJson(File file, Type type) {
        try {
            if (!file.exists()) return null;
            BufferedReader reader = new BufferedReader(new FileReader(file));
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> void writeJson(File file, T data) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            gson.toJson(data, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
