package controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Application configuration classes
 * It is used to manage configuration items such as API keys and URLs
 */
public class AppConfig {
    private static final String CONFIG_FILE = "app_config.properties";
    private static Properties properties = new Properties();

    // 配置键
    public static final String API_URL_KEY = "transaction.api.url";
    public static final String API_KEY_KEY = "transaction.api.key";
    public static final String USE_API_KEY = "transaction.use.api";

    // 默认值
    private static final String DEFAULT_API_URL = "https://api.deepseek.com/v1/chat/completions";

    static {
        loadConfig();
    }

    /**
     * Load the configuration
     */
    private static void loadConfig() {
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            properties.load(in);
            System.out.println("The configuration file has been loaded");
        } catch (IOException e) {
            System.out.println("No profile found, default configuration is created");
            initializeDefaultConfig();
        }
    }

    /**
     * Initialize the default configuration
     */
    private static void initializeDefaultConfig() {
        properties.setProperty(API_URL_KEY, DEFAULT_API_URL);
        properties.setProperty(API_KEY_KEY, "");  // Default is empty
        properties.setProperty(USE_API_KEY, "false");  // APIs are not used by default
        saveConfig();
    }

    /**
     * Save the configuration
     */
    public static void saveConfig() {
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "Transaction Tracker Configuration");
            System.out.println("The configuration file has been saved");
        } catch (IOException e) {
            System.err.println("Save configuration file failed:" + e.getMessage());
        }
    }

    /**
     * Obtain the configured value
     * @param key Configure the key
     * @return Configure the value and return null if it doesn't exist
     */
    public static String getConfig(String key) {
        return properties.getProperty(key);
    }

    /**
     * Obtain the configured value
     * @param key Configure the key
     * @param defaultValue Default value
     * @return Configure the value, or return the default value if it does not exist
     */
    public static String getConfig(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Set the configured values
     * @param key Configure the key
     * @param value Configure the value
     */
    public static void setConfig(String key, String value) {
        properties.setProperty(key, value);
        saveConfig();
    }

    /**
     * Obtain whether to use APIs
     * @return Whether to use APIs
     */
    public static boolean isUseAPI() {
        return Boolean.parseBoolean(getConfig(USE_API_KEY, "false"));
    }

    /**
     * Specify whether to use APIs
     * @param use Whether to use APIs
     */
    public static void setUseAPI(boolean use) {
        setConfig(USE_API_KEY, String.valueOf(use));
    }

    /**
     * Get the API URL
     * @return API URL
     */
    public static String getApiUrl() {
        return getConfig(API_URL_KEY, DEFAULT_API_URL);
    }

    /**
     * Set the API URL
     * @param url API URL
     */
    public static void setApiUrl(String url) {
        setConfig(API_URL_KEY, url);
    }

    /**
     * Obtain the API key
     * @return API key
     */
    public static String getApiKey() {
        return getConfig(API_KEY_KEY, "");
    }

    /**
     * Set the API key
     * @param key API key
     */
    public static void setApiKey(String key) {
        setConfig(API_KEY_KEY, key);
    }
}