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
    /**
     * 程序入口点，解析命令行参数并运行各种测试
     * 
     * @param args 命令行参数：--threads 线程数 --iterations 每个线程的迭代次数
     * @throws Exception 如果在执行过程中发生异常
     */
    public static void main(String[] args) throws Exception {
        // 默认线程数为CPU核心数（至少为2），默认迭代次数为200万
        int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
        int iterations = 2_000_000;

        // 解析命令行参数
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

        // 计算期望值：线程数 × 迭代次数
        long expected = (long) threads * (long) iterations;
        System.out.printf(Locale.ROOT, "threads=%d iterations=%d expected=%d%n", threads, iterations, expected);

        // 依次运行四种不同的实现方式
        runBroken(threads, iterations, expected);
        runSynchronized(threads, iterations, expected);
        runAtomicInteger(threads, iterations, expected);
        runLongAdder(threads, iterations, expected);
    }

    /**
     * 演示存在数据竞争的不安全实现
     * 
     * @param threads 线程数量
     * @param iterations 每个线程执行的迭代次数
     * @param expected 期望的结果值
     * @throws Exception 如果在执行过程中发生异常
     */
    private static void runBroken(int threads, int iterations, long expected) throws Exception {
        // 定义一个简单的计数器盒子类
        class Box {
            int value; 
        }
        Box box = new Box();

        // 并行执行多个线程，每个线程都对共享变量执行递增操作
        Result r = run(threads, () -> {
            for (int i = 0; i < iterations; i++) box.value++;
        });

        // 输出实际结果、是否正确以及执行时间
        System.out.printf(Locale.ROOT, "broken      actual=%d ok=%s time=%dms%n",
                box.value, (box.value == expected), r.elapsedMillis);
    }

    /**
     * 使用 synchronized 关键字修复数据竞争问题
     * 
     * @param threads 线程数量
     * @param iterations 每个线程执行的迭代次数
     * @param expected 期望的结果值
     * @throws Exception 如果在执行过程中发生异常
     */
    private static void runSynchronized(int threads, int iterations, long expected) throws Exception {
        // 定义一个带同步机制的计数器盒子类
        class Box {
            int value;
            // 对递增方法加锁，确保原子性
            synchronized void inc() { value++; }
        }
        Box box = new Box();

        // 并行执行多个线程，每个线程都调用同步的递增方法
        Result r = run(threads, () -> {
            for (int i = 0; i < iterations; i++) box.inc();
        });

        // 输出实际结果、是否正确以及执行时间
        System.out.printf(Locale.ROOT, "synchronized actual=%d ok=%s time=%dms%n",
                box.value, (box.value == expected), r.elapsedMillis);
    }

    /**
     * 使用 AtomicInteger 原子类修复数据竞争问题
     * 
     * @param threads 线程数量
     * @param iterations 每个线程执行的迭代次数
     * @param expected 期望的结果值
     * @throws Exception 如果在执行过程中发生异常
     */
    private static void runAtomicInteger(int threads, int iterations, long expected) throws Exception {
        // 创建 AtomicInteger 实例作为线程安全的计数器
        AtomicInteger counter = new AtomicInteger();

        // 并行执行多个线程，每个线程都调用原子递增方法
        Result r = run(threads, () -> {
            for (int i = 0; i < iterations; i++) counter.incrementAndGet();
        });

        // 获取最终计数值
        int actual = counter.get();
        // 输出实际结果、是否正确以及执行时间
        System.out.printf(Locale.ROOT, "atomic      actual=%d ok=%s time=%dms%n",
                actual, (actual == expected), r.elapsedMillis);
    }

    /**
     * 使用 LongAdder 原子类修复数据竞争问题（高性能版本）
     * 
     * @param threads 线程数量
     * @param iterations 每个线程执行的迭代次数
     * @param expected 期望的结果值
     * @throws Exception 如果在执行过程中发生异常
     */
    private static void runLongAdder(int threads, int iterations, long expected) throws Exception {
        // 创建 LongAdder 实例作为高性能的线程安全计数器
        LongAdder adder = new LongAdder();

        // 并行执行多个线程，每个线程都调用递增方法
        Result r = run(threads, () -> {
            for (int i = 0; i < iterations; i++) adder.increment();
        });

        // 获取最终计数值（求和）
        long actual = adder.sum();
        // 输出实际结果、是否正确以及执行时间
        System.out.printf(Locale.ROOT, "longAdder   actual=%d ok=%s time=%dms%n",
                actual, (actual == expected), r.elapsedMillis);
    }

    /**
     * 执行多线程任务的核心方法
     * 
     * @param threads 线程数量
     * @param task 要执行的任务
     * @return 包含执行时间的结果对象
     * @throws Exception 如果在执行过程中发生异常
     */
    private static Result run(int threads, ThrowingRunnable task) throws Exception {
        // 创建起始门闩（用于同时启动所有线程）
        CountDownLatch start = new CountDownLatch(1);
        // 创建结束门闩（用于等待所有线程完成）
        CountDownLatch done = new CountDownLatch(threads);

        // 创建固定大小的线程池
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try (pool) {
            // 提交指定数量的任务到线程池
            for (int t = 0; t < threads; t++) {
                pool.execute(() -> {
                    try {
                        // 等待起始信号
                        start.await();
                        // 执行任务
                        task.run();
                    } catch (Exception ignored) {
                        // 忽略任务执行中的异常
                    } finally {
                        // 标记当前线程已完成
                        done.countDown();
                    }
                });
            }

            // 记录开始时间
            long begin = System.nanoTime();
            // 发出起始信号，所有线程同时开始执行
            start.countDown();
            // 等待所有线程完成（最多等待60秒）
            if (!done.await(60, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout waiting for tasks");
            // 计算执行时间（毫秒）
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
            // 返回包含执行时间的结果
            return new Result(elapsedMillis);
        }
    }

    /**
     * 验证命令行参数值是否有效
     * 
     * @param flag 参数标志
     * @param value 参数值
     * @return 有效的参数值
     */
    private static String requireValue(String flag, String value) {
        if (value == null || value.startsWith("--")) throw new IllegalArgumentException("Missing value for " + flag);
        return value;
    }

    /**
     * 函数式接口，表示可能抛出异常的可运行任务
     */
    @FunctionalInterface
    interface ThrowingRunnable {
        /**
         * 执行任务
         * 
         * @throws Exception 如果在执行过程中发生异常
         */
        void run() throws Exception;
    }

    /**
     * 结果记录类，用于存储执行时间
     * 
     * @param elapsedMillis 执行时间（毫秒）
     */
    record Result(long elapsedMillis) {}
}
