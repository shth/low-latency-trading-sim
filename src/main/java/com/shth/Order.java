package com.shth;

public record Order(
        String id,                // unique order ID
        boolean isBuy,            // true = buy, false = sell
        double price,             // limit price
        int quantity              // how many shares
) {
    @Override
    public String toString() {
        return (isBuy ? "BUY" : "SELL") + " " + quantity + "@" + price + " [" + id + "]";
    }
}
