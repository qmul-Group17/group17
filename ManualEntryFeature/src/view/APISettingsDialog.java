package view;

import controller.AppConfig;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import model.Transaction;

/**
 * API设置对话框
 */
public class APISettingsDialog extends JDialog {
    private JTextField apiUrlField;
    private JTextField apiKeyField;
    private JCheckBox useApiCheckBox;

    /**
     * 创建API设置对话框
     * @param parent 父窗口
     */
    public APISettingsDialog(JFrame parent) {
        super(parent, "AI分类API设置", true);
        initComponents();
        loadSettings();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * 初始化组件
     */
    private void initComponents() {
        // 设置布局
        setLayout(new BorderLayout());

        // 创建主面板
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 使用API复选框
        useApiCheckBox = new JCheckBox("启用DeepSeek AI分类API");
        mainPanel.add(useApiCheckBox, gbc);

        // API URL
        gbc.gridy++;
        mainPanel.add(new JLabel("API URL:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        apiUrlField = new JTextField(30);
        mainPanel.add(apiUrlField, gbc);

        // API密钥
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("API密钥:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        apiKeyField = new JTextField(30);
        mainPanel.add(apiKeyField, gbc);

        // 说明文本
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextArea infoArea = new JTextArea(
                "启用DeepSeek AI分类API后，系统将使用DeepSeek的AI服务对交易进行智能分类。\n" +
                        "您需要提供有效的API URL和API密钥。\n" +
                        "如果未启用API或API调用失败，系统将回退到本地分类模式。"
        );
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(mainPanel.getBackground());
        infoArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(infoArea, gbc);

        // 添加主面板
        add(mainPanel, BorderLayout.CENTER);

        // 添加按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
                dispose();
            }
        });

        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        JButton testButton = new JButton("测试连接");
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                testConnection();
            }
        });

        buttonPanel.add(testButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // 启用状态更改监听器
        useApiCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean enabled = useApiCheckBox.isSelected();
                apiUrlField.setEnabled(enabled);
                apiKeyField.setEnabled(enabled);
                testButton.setEnabled(enabled);
            }
        });
    }

    /**
     * 加载配置设置
     */
    private void loadSettings() {
        useApiCheckBox.setSelected(AppConfig.isUseAPI());
        apiUrlField.setText(AppConfig.getApiUrl());
        apiKeyField.setText(AppConfig.getApiKey());

        // 根据启用状态设置字段可编辑性
        boolean enabled = useApiCheckBox.isSelected();
        apiUrlField.setEnabled(enabled);
        apiKeyField.setEnabled(enabled);
    }

    /**
     * 保存配置设置
     */
    private void saveSettings() {
        AppConfig.setUseAPI(useApiCheckBox.isSelected());
        AppConfig.setApiUrl(apiUrlField.getText());
        AppConfig.setApiKey(apiKeyField.getText());

        JOptionPane.showMessageDialog(this,
                "API设置已保存。\n" +
                        (useApiCheckBox.isSelected() ? "系统将使用DeepSeek AI分类API。" : "系统将使用本地分类模式。"),
                "保存成功",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 测试API连接
     */
    private void testConnection() {
        String url = apiUrlField.getText();
        String key = apiKeyField.getText();

        if (url.isEmpty() || key.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请输入API URL和密钥。",
                    "输入错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 暂存当前配置
        String origUrl = AppConfig.getApiUrl();
        String origKey = AppConfig.getApiKey();
        boolean origUse = AppConfig.isUseAPI();

        try {
            // 临时设置测试配置
            AppConfig.setApiUrl(url);
            AppConfig.setApiKey(key);
            AppConfig.setUseAPI(true);

            // 创建一个测试交易
            Transaction testTransaction = new Transaction(
                    Transaction.Type.EXPENSE,
                    "测试",
                    100.0,
                    java.time.LocalDate.now(),
                    "超市购物",
                    "测试来源"
            );

            // 尝试调用API
            String result = testAPIConnection(testTransaction);

            JOptionPane.showMessageDialog(this,
                    "API连接测试成功！\n响应: " + result,
                    "测试成功",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "API连接测试失败: " + e.getMessage(),
                    "测试失败",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            // 恢复原始配置
            AppConfig.setApiUrl(origUrl);
            AppConfig.setApiKey(origKey);
            AppConfig.setUseAPI(origUse);
        }
    }

    /**
     * 测试API连接
     * @param transaction 测试交易
     * @return API响应
     */
    private String testAPIConnection(Transaction transaction) {
        try {
            // 创建交易描述
            String description = transaction.getNote();
            if (description == null || description.isEmpty()) {
                description = transaction.getSource();
            }

            double amount = transaction.getAmount();
            String date = transaction.getDate() != null ? transaction.getDate().toString() : "";
            String type = transaction.getType().toString();

            // 创建API请求
            StringBuilder prompt = new StringBuilder();
            prompt.append("分析以下交易信息，并将其分类到最合适的类别中:\n\n");
            prompt.append("交易描述: ").append(description).append("\n");
            prompt.append("金额: ").append(amount).append("\n");
            prompt.append("日期: ").append(date).append("\n");
            prompt.append("类型: ").append(type).append("\n\n");
            prompt.append("请从以下类别中选择一个最合适的: 餐饮, 交通, 购物, 健康, 旅游, 美妆, 娱乐, 转账, 住房, 人情社交, 教育, 通讯, 红包, 投资, 借出, 还款, 亲子, 宠物, 其他");
            prompt.append("\n\n只需回复类别名称，不要其他内容。");

            // 构建请求体
            String requestBody = "{"
                    + "\"model\": \"deepseek-chat\","
                    + "\"messages\": ["
                    + "  {\"role\": \"user\", \"content\": \"" + escapeJson(prompt.toString()) + "\"}"
                    + "],"
                    + "\"temperature\": 0.0,"
                    + "\"max_tokens\": 10"
                    + "}";

            // 获取配置的URL和密钥
            String apiUrl = apiUrlField.getText();
            String apiKey = apiKeyField.getText();

            // 发送API请求
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

            // 读取响应
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return "API连接成功！响应: " + response.toString();
                }
            } else {
                throw new RuntimeException("API调用失败，HTTP错误码: " + responseCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("API调用过程中发生异常: " + e.getMessage(), e);
        }
    }

    /**
     * 转义JSON字符串
     */
    private String escapeJson(String input) {
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