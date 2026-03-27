package com.shth;

import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CASDemo {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("=== Compare-and-Swap (CAS) Demo ===");

        // === Step 1: Using AtomicLong (Java’s built-in CAS wrapper) ===
        AtomicLong counter = new AtomicLong(0);

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            for (int i = 0; i < 100; i++) {                 // 100 virtual threads
                scope.fork(() -> {
                    for (int j = 0; j < 10_000; j++) {      // each adds 10k times
                        counter.incrementAndGet();          // ← this is CAS under the hood!
                    }
                    return null;
                });
            }
            scope.join().throwIfFailed();
        }

        System.out.println("Final counter with CAS: " + counter.get()); // must be exactly 1_000_000

        // === Step 2: Manual CAS loop (what AtomicLong actually does inside) ===
        AtomicLong manualCounter = new AtomicLong(0);

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            for (int i = 0; i < 100; i++) {
                scope.fork(() -> {
                    for (int j = 0; j < 10_000; j++) {
                        long oldValue;
                        do {
                            oldValue = manualCounter.get();                    // read
                            // "If still oldValue, swap to oldValue+1"
                        } while (!manualCounter.compareAndSet(oldValue, oldValue + 1)); // ← pure CAS!
                    }
                    return null;
                });
            }
            scope.join().throwIfFailed();
        }
        System.out.println("Final counter with manual CAS loop: " + manualCounter.get());

        // === Step 3: Manual CAS loop (what AtomicLong actually does inside) ===
        final Long[] nonThreadSafeCounter = {0L};

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            for (int i = 0; i < 100; i++) {
                scope.fork(() -> {
                    for (int j = 0; j < 10_000; j++) {
                        nonThreadSafeCounter[0] = nonThreadSafeCounter[0] + 1;
                    }
                    return null;
                });
            }
            scope.join().throwIfFailed();
        }
        System.out.println("Final counter with non thread-safe: " + nonThreadSafeCounter[0]);

        System.out.println("Both demos finished in ~nanoseconds per update — no locks used!");
    }
}