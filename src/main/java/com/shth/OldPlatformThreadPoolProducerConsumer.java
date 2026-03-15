package com.shth;

import java.util.concurrent.*;

public class OldPlatformThreadPoolProducerConsumer {

    private static final int NUM_ITEMS = 1000000;
    private static final int NUM_CONSUMERS = 500;
    private static final int QUEUE_CAPACITY = 10_000;
    private static final int THINK_TIME_MICROS = 1;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("\n=== Platform thread without thread pool  ===");
        long platformMs = runDemo(false);
        double platformThroughput = NUM_ITEMS / (platformMs / 1000.0);
        System.out.println("Time: " + platformMs + " ms");
        System.out.println("Throughput: " + String.format("%.0f", platformThroughput) + " items/second");

        System.out.println("\n=== Platform thread with thread pool  ===");
        long virtualMs = runDemo(true);
        double virtualThroughput = NUM_ITEMS / (virtualMs / 1000.0);
        System.out.println("Time: " + virtualMs + " ms");
        System.out.println("Throughput: " + String.format("%.0f", virtualThroughput) + " items/second");
    }

    private static long runDemo(boolean useThreadPool) throws InterruptedException {
        if (useThreadPool) {
            return withThreadPool();
        } else {
            return withoutThreadPool();
        }
    }

    public static long withoutThreadPool() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        long start = System.nanoTime();

        // Producer task
        Runnable producerTask = () -> {
            try {
                for (int i = 0; i < NUM_ITEMS; i++) {
                    queue.put(i);                 // send number
                }
                // Poison pills to tell consumers "stop"
                for (int i = 0; i < NUM_CONSUMERS; i++) {
                    queue.put(-1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        // Start producer
        Thread producer = Thread.ofPlatform().start(producerTask);

        // Start many consumers
        Thread[] consumers = new Thread[NUM_CONSUMERS];
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            Runnable consumerTask = () -> {
                try {
                    while (true) {
                        Integer item = queue.take();   // wait and take
                        if (item == -1) break;         // stop signal
                        // Simulate fast order processing here
                        Thread.sleep(THINK_TIME_MICROS, 0);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };

            consumers[i] = Thread.ofPlatform().start(consumerTask);
        }

        // Wait for everyone to finish
        producer.join();
        for (Thread c : consumers) c.join();

        long end = System.nanoTime();
        return (end - start) / 1_000_000;   // convert to milliseconds
    }

    public static long withThreadPool() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();   // or newFixedThreadPool(500) for safety

        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        long start = System.nanoTime();

        // Producer (runs on one platform thread)
        executor.submit(() -> {
            try {
                for (int i = 0; i < NUM_ITEMS; i++) {
                    queue.put(i);
                }
                for (int i = 0; i < NUM_CONSUMERS; i++) {
                    queue.put(-1); // poison pill
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Start 500 consumers using the thread pool (old way)
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            executor.submit(() -> {
                try {
                    while (true) {
                        Integer item = queue.take();
                        if (item == -1) break;
                        // Simulate tiny trading work
                        Thread.sleep(0, THINK_TIME_MICROS * 1_000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Wait for the boss to finish all jobs
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long end = System.nanoTime();
        return (end - start) / 1_000_000;   // convert to milliseconds
    }
}