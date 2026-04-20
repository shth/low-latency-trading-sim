package com.shth;   // ← keep your package name (or delete this line if you don't use packages)

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class ChronicleOrderQueue implements AutoCloseable {

    private final ChronicleQueue queue;
    private final ExcerptAppender appender;
    private final ExcerptTailer tailer;

    // Constructor – now using the official recommended builder
    public ChronicleOrderQueue(String basePath) {
        // This creates the off-heap + persistent queue on disk
        this.queue = ChronicleQueue.singleBuilder(basePath).build();
        this.appender = queue.createAppender();   // writer (fixed!)
        this.tailer = queue.createTailer();        // reader
        System.out.println("✅ Chronicle Queue created at: " + basePath);
    }

    // Write one order (producer)
    public void appendOrder(String orderMessage) {
        appender.writeText(orderMessage);
    }

    // Read the next order (consumer)
    public String readNextOrder() {
        return tailer.readText();
    }

    // Auto-close everything safely (required for try-with-resources)
    @Override
    public void close() {
        if (queue != null) {
            queue.close();
            System.out.println("✅ Chronicle Queue closed cleanly.");
        }
    }

    // Simple benchmark – 10,000 write + read cycles
    public long runBenchmark(int cycles) {
        long start = System.nanoTime();

        for (int i = 0; i < cycles; i++) {
            String fakeOrder = "Order-" + i + " BUY AAPL 100 @ 150.25";
            appendOrder(fakeOrder);
            String readBack = readNextOrder();   // read it straight back
        }

        long end = System.nanoTime();
        long durationNs = end - start;
        System.out.println("10k write+read cycles took: " +
                TimeUnit.NANOSECONDS.toMicros(durationNs) + " µs");
        System.out.println("Average per cycle: " +
                (durationNs / cycles) + " ns");
        return durationNs;
    }

    // Run this class alone to test
    public static void main(String[] args) {
        String path = Paths.get("target", "chronicle-queue-data").toString();
        try (ChronicleOrderQueue q = new ChronicleOrderQueue(path)) {
            q.runBenchmark(10_000);
        }
    }
}