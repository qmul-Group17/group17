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

    // 支持的日期格式
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

            // 读取CSV文件
            while ((line = br.readLine()) != null) {
                // 跳过表头
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                try {
                    // 首先尝试使用简单的逗号分隔
                    String[] parts = line.split(",");

                    // 如果分割结果不够，尝试使用更复杂的CSV解析
                    if (parts.length < 4) {
                        parts = parseCSVLine(line);
                    }

                    if (parts.length < 4) {
                        System.err.println("跳过无效行: " + line);
                        continue;
                    }

                    // 解析类型
                    Type type = parseType(parts[0]);

                    // 解析金额 (先保留原始分类，后面会由AI重新分类)
                    String category = parts.length > 1 ? parts[1].trim() : "Pending";

                    // 解析金额
                    double amount = parseAmount(parts[2]);

                    // 解析日期
                    LocalDate date = parseDate(parts[3]);

                    // 解析备注和来源
                    String note = parts.length > 4 ? parts[4].trim() : "";
                    String source = parts.length > 5 ? parts[5].trim() : "CSV导入";

                    // 创建交易记录 - 注意这里不直接分类，保留原始分类或使用"Pending"
                    // 后续会在MainFrame中使用TransactionCategorizer进行分类
                    // 不对交易进行立即分类，让控制器处理分类
                    Transaction transaction = new Transaction(type, "待分类", amount, date, note, source);
                    transactions.add(transaction);

                } catch (Exception e) {
                    System.err.println("解析行时出错: " + line + ", 错误: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("导入CSV失败: " + e.getMessage());
        }

        return transactions;
    }

    /**
     * 从CSV文件导入交易
     * @param filePath CSV文件路径
     * @return 导入的交易列表
     */
    public List<Transaction> importTransactions(String filePath) {
        List<Transaction> transactions = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                // 跳过表头
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // 解析CSV行
                String[] values = line.split(",");
                if (values.length >= 6) {
                    try {
                        // 创建交易对象
                        Transaction.Type type = Transaction.Type.valueOf(values[0].trim());
                        String category = values[1].trim();
                        double amount = Double.parseDouble(values[2].trim());
                        LocalDate date = LocalDate.parse(values[3].trim());
                        String note = values[4].trim();
                        String source = values[5].trim();

                        Transaction transaction = new Transaction(type, category, amount, date, note, source);
                        transactions.add(transaction);
                    } catch (Exception e) {
                        System.err.println("解析CSV行失败: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取CSV文件失败: " + e.getMessage());
        }

        return transactions;
    }

    /**
     * 解析CSV行（处理引号内的逗号）
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
     * 解析交易类型
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
     * 解析金额
     */
    private static double parseAmount(String value) {
        // 移除货币符号和逗号
        String normalized = value.replaceAll("[¥$,]", "").trim();
        return Double.parseDouble(normalized);
    }

    /**
     * 尝试用不同格式解析日期
     */
    private static LocalDate parseDate(String value) {
        String normalized = value.trim();

        // 首先尝试直接解析
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ignored) {
            // 如果直接解析失败，尝试其他格式
        }

        // 尝试不同的日期格式
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一个格式
            }
        }

        // 如果所有格式都失败，使用当前日期
        System.err.println("无法解析日期: " + value + ", 使用当前日期");
        return LocalDate.now();
    }
}