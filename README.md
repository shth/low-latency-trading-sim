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