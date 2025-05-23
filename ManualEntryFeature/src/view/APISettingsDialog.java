package view;

import controller.AppConfig;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import model.Transaction;

/**
 * API Settings Dialog
 */
public class APISettingsDialog extends JDialog {
    private JTextField apiUrlField;
    private JTextField apiKeyField;
    private JCheckBox useApiCheckBox;

    /**
     * Create the API settings dialog
     * @param parent The parent window
     */
    public APISettingsDialog(JFrame parent) {
        super(parent, "AI Classification API Settings", true);
        initComponents();
        loadSettings();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Initialize components
     */
    private void initComponents() {
        // Set layout
        setLayout(new BorderLayout());

        // Create main panel
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Use API checkbox
        useApiCheckBox = new JCheckBox("Enable DeepSeek AI Classification API");
        mainPanel.add(useApiCheckBox, gbc);

        // API URL
        gbc.gridy++;
        mainPanel.add(new JLabel("API URL:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        apiUrlField = new JTextField(30);
        mainPanel.add(apiUrlField, gbc);

        // API Key
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("API Key:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        apiKeyField = new JTextField(30);
        mainPanel.add(apiKeyField, gbc);

        // Information text
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextArea infoArea = new JTextArea(
                "After enabling the DeepSeek AI Classification API, the system will use DeepSeek's AI service to intelligently classify transactions.\n" +
                        "You need to provide a valid API URL and API Key.\n" +
                        "If the API is not enabled or the API call fails, the system will fall back to the local classification mode."
        );
        infoArea.setEditable(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setBackground(mainPanel.getBackground());
        infoArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(infoArea, gbc);

        // Add the main panel
        add(mainPanel, BorderLayout.CENTER);

        // Add button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
                dispose();
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        JButton testButton = new JButton("Test Connection");
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

        // Enable/disable fields based on checkbox state
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
     * Load configuration settings
     */
    private void loadSettings() {
        useApiCheckBox.setSelected(AppConfig.isUseAPI());
        apiUrlField.setText(AppConfig.getApiUrl());
        apiKeyField.setText(AppConfig.getApiKey());

        // Set field enablement based on the enabled state
        boolean enabled = useApiCheckBox.isSelected();
        apiUrlField.setEnabled(enabled);
        apiKeyField.setEnabled(enabled);
    }

    /**
     * Save the configuration settings
     */
    private void saveSettings() {
        AppConfig.setUseAPI(useApiCheckBox.isSelected());
        AppConfig.setApiUrl(apiUrlField.getText());
        AppConfig.setApiKey(apiKeyField.getText());

        JOptionPane.showMessageDialog(this,
                "API settings have been saved.\n" +
                        (useApiCheckBox.isSelected() ? "The system will use the DeepSeek AI Classification API." : "The system will use the local classification mode."),
                "Save Successful",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Test the API connection
     */
    private void testConnection() {
        String url = apiUrlField.getText();
        String key = apiKeyField.getText();

        if (url.isEmpty() || key.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the API URL and Key.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Temporarily save the current configuration
        String origUrl = AppConfig.getApiUrl();
        String origKey = AppConfig.getApiKey();
        boolean origUse = AppConfig.isUseAPI();

        try {
            // Temporarily set the test configuration
            AppConfig.setApiUrl(url);
            AppConfig.setApiKey(key);
            AppConfig.setUseAPI(true);

            // Create a test transaction
            Transaction testTransaction = new Transaction(
                    Transaction.Type.EXPENSE,
                    "Test",
                    100.0,
                    java.time.LocalDate.now(),
                    "Supermarket Shopping",
                    "Test Source"
            );

            // Try to call the API
            String result = testAPIConnection(testTransaction);

            JOptionPane.showMessageDialog(this,
                    "API connection test successful!\nResponse: " + result,
                    "Test Successful",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "API connection test failed: " + e.getMessage(),
                    "Test Failed",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            // Restore the original configuration
            AppConfig.setApiUrl(origUrl);
            AppConfig.setApiKey(origKey);
            AppConfig.setUseAPI(origUse);
        }
    }

    /**
     * Test the API connection
     * @param transaction The test transaction
     * @return The API response
     */
    private String testAPIConnection(Transaction transaction) {
        try {
            // Create transaction description
            String description = transaction.getNote();
            if (description == null || description.isEmpty()) {
                description = transaction.getSource();
            }

            double amount = transaction.getAmount();
            String date = transaction.getDate() != null ? transaction.getDate().toString() : "";
            String type = transaction.getType().toString();

            // Create the API request
            StringBuilder prompt = new StringBuilder();
            prompt.append("Analyze the following transaction information and classify it into the most suitable category:\n\n");
            prompt.append("Transaction description: ").append(description).append("\n");
            prompt.append("Amount: ").append(amount).append("\n");
            prompt.append("Date: ").append(date).append("\n");
            prompt.append("Type: ").append(type).append("\n\n");
            prompt.append("Please choose the most appropriate category from the following: Food, Transport, Shopping, Health, Travel, Beauty, Entertainment, Transfer, Housing, Social, Education, Communication, Red Packet, Investment, Lending, Repayment, Parenting, Pet, Other");
            prompt.append("\n\nPlease respond with only the category name, no other content.");

            // Build the request body
            String requestBody = "{"
                    + "\"model\": \"deepseek-chat\","
                    + "\"messages\": ["
                    + "  {\"role\": \"user\", \"content\": \"" + escapeJson(prompt.toString()) + "\"}"
                    + "],"
                    + "\"temperature\": 0.0,"
                    + "\"max_tokens\": 10"
                    + "}";

            // Get the configured URL and key
            String apiUrl = apiUrlField.getText();
            String apiKey = apiKeyField.getText();

            // Send the API request
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

            // Read the response
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return "API connection successful! Response: " + response.toString();
                }
            } else {
                throw new RuntimeException("API call failed, HTTP error code: " + responseCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("An error occurred during the API call: " + e.getMessage(), e);
        }
    }

    /**
     * Escape JSON string
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