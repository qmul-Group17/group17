
# Personal Finance Tracker - Core Development Phase

This project is a personal finance tracker built with Java, focusing on foundational functionality for recording and managing individual transactions. This repository reflects the **first phase** of development, along with partial progress into the second phase.

## Phase 1: Core Functionalities

### ✅ US1: Manual Expense Entry
- **Description**: Allows users to manually input expenses and income.
- **Implemented Features**:
  - Input fields: type (expense/income), category, amount, date, note, and source.
  - Category presets for income and expense (with the ability to add custom ones).
  - Local data persistence using a built-in `.json` file to simulate a simple database.
- **Status**: Completed

### ✅ US4: Multi-Currency Support
- **Description**: In the manual addition of transactions, add the option of amount unit to support multi-currency input.
- **Implemented Features**:
  - When manually entering the transaction amount, users can select Chinese Yuan (CNY), US Dollar (USD), British Pound (GBP), Euro (EUR), Japanese Yen (JPY), and South Korean Won (KRW), which will be automatically converted to RMB according to the proposed exchange rate and finally displayed as RMB.
  - Stored and exported along with each transaction.
- **Status**: Completed (basic field-level support, no conversion logic yet)



### ✅ US10: Bill Export Function
- **Description**: Allows users to export their financial data (transactions) to a CSV file for external use, reporting, or backup purposes.
- **Implemented Features**: 
  - **Export to CSV**: Users can export all transaction data, or a filtered subset, into a CSV format. The CSV file includes details such as transaction type (income/expense), category, amount, date, description, and source.
  - **User-Friendly Interface**: A button labeled "Export CSV" is provided within the main application interface. Upon clicking this button, the user is prompted to choose a file location to save the CSV file.
  - **Automatic Data Generation**: The exported file automatically includes all data currently stored in the system. The system ensures that each entry in the CSV file matches the current structure of the transaction data, including any manual entries, imported transactions, and modified data.
  - **Customizable Filters**: Users can apply filters to export only specific categories, date ranges, or transaction types (e.g., only expenses or only income). This allows for more granular data extraction, enabling users to generate customized reports.
  - **File Format**: The file is saved in the standard CSV format, which can be opened and manipulated using spreadsheet software such as Microsoft Excel or Google Sheets.
  - **Support for Large Datasets**: The export function is optimized to handle a large number of transactions, ensuring that the file generation remains responsive even with substantial data volumes.
- **Status**: Completed (basic version)

  **Use Case**:
  - A user wants to create a monthly financial report or submit tax-related information. They can export their transactions for that month or year to a CSV file and open it in Excel to analyze or print it.

## Phase 2: Data Input & Categorization

### ✅ US3: Import CSV Files
- **Description**: Enables importing external transaction records via CSV.
- **Status**: Completed

### ✅ US7: AI Auto Categorization
- **Description**: Uses a dual-tier intelligent categorization system to automatically classify transactions.
    1.Simple Mode: Keywords-based pattern matching for basic categorization
    2.API Mode: DeepSeek Chat API for high-accuracy AI-powered categorization
- **Implemented Features**: 
    1.Automatic categorization during transaction import and manual entry
    2.User correction tracking to improve future categorization accuracy
    3.AI classification through DeepSeek API that analyzes transaction description, amount, date, and source
    4.Toggle between simple keyword mode and API mode
    5.Configuration panel for API settings (URL and key)
- **Planned Upgrade**:Planning to add AI-powered expense analysis and summary reports
- **Status**: Completed (basic version)


### ⏳ US9: Manual Category Adjustment
- **Description**: Allows users to manually correct or modify the category of an automatically categorized transaction.
- **Status**: Not yet implemented

## Additional Contributions

### ✅ Data Visualization & UI Refinement
This section was independently developed and implemented.

- Improved the overall UI layout for a cleaner and more intuitive experience.
- Developed visual data analytics, including:
  - 📊 A **pie chart** that illustrates the distribution of expenses by category.
  - 📈 A **line chart** that tracks daily spending trends over time.
- These charts are embedded within the application interface to assist users in understanding their financial habits more effectively.
- **Planned Upgrade**: Planning to enable clickable categories that allow users to view all income and expense records under each category. Also planning to support rendering line charts for a specific month or a selected time range.
- **Status**: Currently implemented global **pie chart** for expense category distribution and **line chart** for daily spending trends.

## How to Run

1. Clone the repo  
   `git clone https://github.com/your-org/personal-finance-tracker.git`

2. Open in your IDE (e.g., IntelliJ or Eclipse)

3. Compile:
   - `javac -cp "lib/gson-2.10.1.jar;lib/jfreechart-1.5.3.jar" -d out src/model/*.java src/service/*.java src/controller/*.java src/view/*.java src/Main.java`

4. Run:
   - `java -cp "out;lib/gson-2.10.1.jar;lib/jfreechart-1.5.3.jar" Main`

## Team Collaboration & Division

| Team Member   | Contributions                                                               |
|---------------|-----------------------------------------------------------------------------|
| Jingxi Linag  | US1 Manual entry and local storage with JSON                                |
| Pengzhu Guo   | US4 Multi-Currency Support                                                  |
| Shumeng Liang | US3 Import CSV Files                                                        |
| Yijing Wang   | US7 Auto-categorization, add AI-powered expense analysis and summary reports|
| Xiwen Zheng   | Data visualization with charts, UI/UX refinement                            |
| Jiawen Zhang  | US10 Bill Export Function                                                        |
