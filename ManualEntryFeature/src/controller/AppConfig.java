package controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 应用程序配置类
 * 用于管理API密钥、URL等配置项
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
     * 加载配置
     */
    private static void loadConfig() {
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            properties.load(in);
            System.out.println("已加载配置文件");
        } catch (IOException e) {
            System.out.println("未找到配置文件，将创建默认配置");
            initializeDefaultConfig();
        }
    }

    /**
     * 初始化默认配置
     */
    private static void initializeDefaultConfig() {
        properties.setProperty(API_URL_KEY, DEFAULT_API_URL);
        properties.setProperty(API_KEY_KEY, "");  // 默认为空
        properties.setProperty(USE_API_KEY, "false");  // 默认不使用API
        saveConfig();
    }

    /**
     * 保存配置
     */
    public static void saveConfig() {
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "Transaction Tracker Configuration");
            System.out.println("已保存配置文件");
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
    }

    /**
     * 获取配置值
     * @param key 配置键
     * @return 配置值，如果不存在则返回null
     */
    public static String getConfig(String key) {
        return properties.getProperty(key);
    }

    /**
     * 获取配置值
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果不存在则返回默认值
     */
    public static String getConfig(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 设置配置值
     * @param key 配置键
     * @param value 配置值
     */
    public static void setConfig(String key, String value) {
        properties.setProperty(key, value);
        saveConfig();
    }

    /**
     * 获取是否使用API
     * @return 是否使用API
     */
    public static boolean isUseAPI() {
        return Boolean.parseBoolean(getConfig(USE_API_KEY, "false"));
    }

    /**
     * 设置是否使用API
     * @param use 是否使用API
     */
    public static void setUseAPI(boolean use) {
        setConfig(USE_API_KEY, String.valueOf(use));
    }

    /**
     * 获取API URL
     * @return API URL
     */
    public static String getApiUrl() {
        return getConfig(API_URL_KEY, DEFAULT_API_URL);
    }

    /**
     * 设置API URL
     * @param url API URL
     */
    public static void setApiUrl(String url) {
        setConfig(API_URL_KEY, url);
    }

    /**
     * 获取API密钥
     * @return API密钥
     */
    public static String getApiKey() {
        return getConfig(API_KEY_KEY, "");
    }

    /**
     * 设置API密钥
     * @param key API密钥
     */
    public static void setApiKey(String key) {
        setConfig(API_KEY_KEY, key);
    }
}