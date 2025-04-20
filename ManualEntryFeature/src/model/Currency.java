package model;

public enum Currency {
    CNY("Chinese Yuan"),
    USD("US Dollar"),
    EUR("Euro"),
    GBP("British Pound"),
    JPY("Japanese Yen"),
    KRW("South Korean Won");

    private final String displayName;

    Currency(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName + " (" + name() + ")";
    }
}