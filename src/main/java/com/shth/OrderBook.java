package com.shth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap; // thread-safe map for later concurrency

public class OrderBook {
    // price -> list of orders at that exact price
    private final Map<Double, List<Order>> buySide = new ConcurrentHashMap<>();
    private final Map<Double, List<Order>> sellSide = new ConcurrentHashMap<>();

    public void addOrder(Order order) {
        var side = order.isBuy() ? buySide : sellSide;
        side.computeIfAbsent(order.price(), k -> new ArrayList<>()).add(order);
        // In real low-latency we would keep sides sorted, but for Week 2 this is enough
    }

    public void printBook() {
        System.out.println("\n=== ORDER BOOK ===");
        System.out.println("BUY SIDE (highest price first):");
        buySide.entrySet().stream()
                .sorted(Map.Entry.<Double, List<Order>>comparingByKey().reversed())
                .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));

        System.out.println("SELL SIDE (lowest price first):");
        sellSide.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));
    }
}