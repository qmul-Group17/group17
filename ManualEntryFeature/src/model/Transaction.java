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

    public Transaction(Type type, String category, double amount, LocalDate date, String note, String source) {
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.note = note;
        this.source = source;
    }

    public Type getType() { return type; }
    public String getCategory() { return category; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getNote() { return note; }
    public String getSource() { return source; }
}