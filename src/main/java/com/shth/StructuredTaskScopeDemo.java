package com.shth;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

public class StructuredTaskScopeDemo {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        OrderBook book = new OrderBook();

        long start = System.nanoTime();
        // We run 15 concurrent "order processors" as a safe group
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {  // auto-cancels on any failure

            // Fork 15 subtasks (each processes one fake order)
            List<StructuredTaskScope.Subtask<Object>> subtasks = IntStream.range(0, 15)
                    .mapToObj(i -> scope.fork(() -> {
                        // Simulate low-latency work: create & add a random order
                        Thread.sleep((long) (Math.random() * 10)); // tiny random delay
                        Order order = new Order(
                                "ORD-" + Thread.currentThread().threadId() + "-" + i,
                                Math.random() > 0.5,           // random buy/sell
                                100.0 + Math.random() * 10,    // price between 100-110
                                (int) (10 + Math.random() * 90) // quantity 10-100
                        );
                        book.addOrder(order);
                        System.out.println("[" + Thread.currentThread().getName() + "] Added: " + order);
                        return null;
                    }))
                    .toList();

            // Wait for the whole group safely
            scope.join().throwIfFailed();   // if any subtask fails, all others are cancelled automatically

            // All done – print the book
            book.printBook();
            System.out.println("\nSUCCESS: 15 orders processed concurrently with StructuredTaskScope!");
        }

        long latencyUs = (System.nanoTime() - start) / 1_000;
        System.out.println("Total time for 15 concurrent orders: " + latencyUs + " µs");
    }
}