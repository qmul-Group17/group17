package model;

import java.time.LocalDate;

public class Transaction {
    public enum Type {
        INCOME, EXPENSE
    }

    private Type type;
    private String category;
    private double amount;
    private LocalDate date;
    private String note;
    private String source;
    private LocalDate editTime;

    // 6-parameter constructor (for code compatibility)
    public Transaction(Type type, String category, double amount, LocalDate date, String note, String source) {
        this(type, category, amount, date, note, source, LocalDate.now());
    }

    // 7-parameter constructor (for Gson deserialization)
    public Transaction(Type type, String category, double amount, LocalDate date,
                       String note, String source, LocalDate editTime) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.source = source;
        this.editTime = (editTime != null) ? editTime : LocalDate.now();
    }

    // Getters and Setters
    public Type getType() { return type; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public String getSource() { return source; }
    public LocalDate getEditTime() { return editTime; }
    public void setEditTime(LocalDate editTime) {
        this.editTime = editTime;
    }
}
