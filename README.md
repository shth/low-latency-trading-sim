## Week 1 Summary — Virtual Threads First Concurrency Win
**Date:** 15 March 2026  
**Goal completed:** Environment ready + producer-consumer using virtual threads vs platform threads (100,000 items).  

### Benchmark Results 

**Original Run (10 consumers, NO think time — pure CPU work):**
- Platform Threads: 31 ms → **3,225,806 items/second**
- Virtual Threads: 38 ms → **2,631,579 items/second**  
  (Platform slightly faster — normal for pure calculation)

**Improved Run (500 consumers + 1 µs think time — realistic trading simulation):**
- Platform Threads: 777 ms → **128,700 items/second**
- Virtual Threads: 335 ms → **298,507 items/second** ← **Virtual threads ~2.3× faster**

### Key Learnings 
- Virtual threads are lightweight workers managed by the JVM (you can create millions cheaply). Platform threads are heavy OS threads (only thousands possible before the computer slows down).
- When the work is pure CPU calculation, platform threads perform similarly as virtual thread
- As soon as we add even tiny waiting (1 microsecond “think time”) and more workers (500 consumers), virtual threads win big — exactly what happens in real trading when thousands of orders wait for matching or risk checks.
- Run-to-run variance (jitter) and JIT warm-up are normal in low-latency work. Always run benchmarks a few times and compare stable numbers.
- Before Virtual thread (Java 23), high concurrency can be achieved with thread pool

### Week 2 – Safe Grouped Concurrency + Working Order Book + CAS Deep-Dive
- Introduced **StructuredTaskScope** for clean, grouped concurrency.
  - **StructuredTaskScope** (zero-knowledge explanation): A “team manager” for threads. You start many subtasks together (fork), wait for the whole team (join), and if one fails the manager instantly cancels the rest. No more leaking threads or messy errors — everything shuts down cleanly. In low-latency trading this prevents random micro-delays and memory leaks that ruin p99 numbers.
- Built a thread-safe **OrderBook** class (`Map<Double, List<Order>>` on buy/sell sides) using `ConcurrentHashMap`.
- Added full **Compare-and-Swap (CAS)** demo with `AtomicLong`.
  - **Compare-and-Swap (CAS)** (zero-knowledge explanation): A single CPU instruction that checks “Is the value still what I expect?” and swaps it in one unbreakable step (nanoseconds). No locks, no waiting queues. This is the foundation of every lock-free system in HFT. It lets hundreds of virtual threads update the same number at the same time with almost zero overhead.
- Ran 100 concurrent workers updating a shared position counter → exactly 100 000 updates, zero data races, zero locks.

**Sample output from Week 2 demo (StructuredTaskScope + OrderBook):**
=== Compare-and-Swap (CAS) Demo ===
Final counter with CAS: 1000000
Final counter with manual CAS loop: 1000000
Final counter with non thread-safe: 142789
