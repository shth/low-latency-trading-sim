package com.shth;

import java.util.concurrent.*;

public class VirtualThreadsProducerConsumer {

    private static final int NUM_ITEMS = 100_000;      // 100,000 orders/numbers
    private static final int NUM_CONSUMERS = 500;       // many consumers
    private static final int QUEUE_CAPACITY = 10_000;
    private static final int THINK_TIME_MICROS = 1;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("\n=== Platform Threads  ===");
        long platformMs = runDemo(false);
        double platformThroughput = NUM_ITEMS / (platformMs / 1000.0);
        System.out.println("Time: " + platformMs + " ms");
        System.out.println("Throughput: " + String.format("%.0f", platformThroughput) + " items/second");

        System.out.println("\n=== Virtual Threads  ===");
        long virtualMs = runDemo(true);
        double virtualThroughput = NUM_ITEMS / (virtualMs / 1000.0);
        System.out.println("Time: " + virtualMs + " ms");
        System.out.println("Throughput: " + String.format("%.0f", virtualThroughput) + " items/second");

        System.out.println("\nVirtual threads win! Ready for Week 2.");
    }

    private static long runDemo(boolean useVirtual) throws InterruptedException {
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
        Thread producer = useVirtual
                ? Thread.ofVirtual().start(producerTask)
                : Thread.ofPlatform().start(producerTask);

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

            consumers[i] = useVirtual
                    ? Thread.ofVirtual().start(consumerTask)
                    : Thread.ofPlatform().start(consumerTask);
        }

        // Wait for everyone to finish
        producer.join();
        for (Thread c : consumers) c.join();

        long end = System.nanoTime();
        return (end - start) / 1_000_000;   // convert to milliseconds
    }
}