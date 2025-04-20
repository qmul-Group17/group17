package service;

import model.Currency;

public class CurrencyConverter {

    public double convert(double amount, Currency from, Currency to) {
        if (from == to) return amount;

        // Step 1: Convert from source currency to CNY
        double amountInCNY = amount * getRateToCNY(from);

        // Step 2: Convert from CNY to target currency
        return amountInCNY / getRateToCNY(to);
    }

    private double getRateToCNY(Currency currency) {
        return switch (currency) {
            case USD -> 7.0;
            case EUR -> 8.0;
            case GBP -> 9.0;
            case JPY -> 0.05;
            case KRW -> 0.005;
            case CNY -> 1.0;
        };
    }
}
