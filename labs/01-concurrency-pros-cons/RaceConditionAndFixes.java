import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 第 1 章实验 B：数据竞争（++）与修复方案对比。
 *
 * 观察重点：
 * - 共享变量的 i++ 不是原子操作：多线程下会丢失更新
 * - 修复方式：synchronized / AtomicInteger / LongAdder（高争用吞吐更好，但语义不同）
 *
 * 运行示例：
 *   javac --release 21 RaceConditionAndFixes.java
 *   java RaceConditionAndFixes --threads 8 --iterations 2000000
 */
public class RaceConditionAndFixes {
    public static void main(String[] args) throws Exception {
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
        int iterations = 2_000_000;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            String v = (i + 1 < args.length) ? args[i + 1] : null;
            switch (a) {
                case "--threads" -> {
                    threads = Integer.parseInt(requireValue(a, v).replace("_", ""));
                    i++;
                }
                case "--iterations" -> {
                    iterations = Integer.parseInt(requireValue(a, v).replace("_", ""));
                    i++;
                }
                default -> {
                }
            }
        }

        long expected = (long) threads * (long) iterations;
        System.out.printf(Locale.ROOT, "threads=%d iterations=%d expected=%d%n", threads, iterations, expected);

        runBroken(threads, iterations, expected);
        runSynchronized(threads, iterations, expected);
        runAtomicInteger(threads, iterations, expected);
        runLongAdder(threads, iterations, expected);
    }

    private static void runBroken(int threads, int iterations, long expected) throws Exception {
        class Box { int value; }
        Box box = new Box();

        Result r = run(threads, () -> {
            for (int i = 0; i < iterations; i++) box.value++;
        });

        System.out.printf(Locale.ROOT, "broken      actual=%d ok=%s time=%dms%n",
                box.value, (box.value == expected), r.elapsedMillis);
    }

    private static void runSynchronized(int threads, int iterations, long expected) throws Exception {
        class Box {
            int value;
            synchronized void inc() { value++; }
        }
        Box box = new Box();

        Result r = run(threads, () -> {
            for (int i = 0; i < iterations; i++) box.inc();
        });

        System.out.printf(Locale.ROOT, "synchronized actual=%d ok=%s time=%dms%n",
                box.value, (box.value == expected), r.elapsedMillis);
    }

    private static void runAtomicInteger(int threads, int iterations, long expected) throws Exception {
        AtomicInteger counter = new AtomicInteger();

        Result r = run(threads, () -> {
            for (int i = 0; i < iterations; i++) counter.incrementAndGet();
        });

        int actual = counter.get();
        System.out.printf(Locale.ROOT, "atomic      actual=%d ok=%s time=%dms%n",
                actual, (actual == expected), r.elapsedMillis);
    }

    private static void runLongAdder(int threads, int iterations, long expected) throws Exception {
        LongAdder adder = new LongAdder();

        Result r = run(threads, () -> {
            for (int i = 0; i < iterations; i++) adder.increment();
        });

        long actual = adder.sum();
        System.out.printf(Locale.ROOT, "longAdder   actual=%d ok=%s time=%dms%n",
                actual, (actual == expected), r.elapsedMillis);
    }

    private static Result run(int threads, ThrowingRunnable task) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.execute(() -> {
                    try {
                        start.await();
                        task.run();
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            long begin = System.nanoTime();
            start.countDown();
            if (!done.await(60, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout waiting for tasks");
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
            return new Result(elapsedMillis);
        } finally {
            pool.shutdownNow();
        }
    }

    private static String requireValue(String flag, String value) {
        if (value == null || value.startsWith("--")) throw new IllegalArgumentException("Missing value for " + flag);
        return value;
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    record Result(long elapsedMillis) {}
}
