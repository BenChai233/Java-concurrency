import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class VolatileVisibilityDemo {
    private static final int EXPECTED = 42;

    // 普通状态类，不使用volatile关键字
    private static final class PlainState {
        int data;
        boolean ready;
    }

    // 使用volatile关键字的状态类，确保可见性
    private static final class VolatileState {
        int data;
        volatile boolean ready;
    }

    // 用于记录测试结果的类
    private static final class Result {
        boolean timeout;  // 是否超时
        boolean staleRead;  // 是否发生过期读取
    }

    private enum Mode {
        PUBLICATION,
        STOP_FLAG
    }

    // 操作顺序枚举
    private enum Ordering {
        CORRECT,    // 正确顺序：先写数据，后设置标志
        WRONG       // 错误顺序：先设置标志，后写数据（会产生过期读）
    }

    // 配置类，用于控制测试参数
    private static final class Config {
        long iterations = 10_000;     // 迭代次数（PUBLICATION 模式）
        long timeoutMs = 50;          // join 超时时间（毫秒）
        boolean useVolatile = true;   // 是否使用 volatile flag

        Mode mode = Mode.PUBLICATION;           // 默认测试模式
        Ordering ordering = Ordering.CORRECT;   // 默认操作顺序

        long writerDelayMs = 0;     // WRONG 顺序下用于扩大窗口
        long stopTrials = 200;      // STOP_FLAG 模式下的尝试次数
    }

    public static void main(String[] args) throws InterruptedException {
        // 解析命令行参数
        Config config = parseArgs(args);
        long startNs = System.nanoTime();
        if (config.mode == Mode.PUBLICATION) {
            runPublication(config, startNs);
        } else {
            runStopFlag(config, startNs);
        }
    }

    /**
     * 执行发布模式测试
     * 模拟消息传递场景：写线程写入数据并发布就绪标志，读线程等待就绪标志后读取数据
     */
    private static void runPublication(Config config, long startNs) throws InterruptedException {
        long timeouts = 0;      // 超时计数
        long staleReads = 0;    // 过期读取计数

        for (long i = 0; i < config.iterations; i++) {
            Result result = new Result();
            if (config.useVolatile) {
                VolatileState state = new VolatileState();
                runOnePublicationIteration(config, result, state);
            } else {
                PlainState state = new PlainState();
                runOnePublicationIteration(config, result, state);
            }

            if (result.timeout) {
                timeouts++;
            } else if (result.staleRead) {
                staleReads++;
            }
        }

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        System.out.println("== VolatileVisibilityDemo (mode=PUBLICATION) ==");
        System.out.println("iterations=" + config.iterations
                + ", timeoutMs=" + config.timeoutMs
                + ", useVolatile=" + config.useVolatile
                + ", ordering=" + config.ordering
                + ", writerDelayMs=" + config.writerDelayMs);
        System.out.println("timeouts=" + timeouts + ", staleReads=" + staleReads);
        System.out.println("elapsedMs=" + elapsedMs);
        System.out.println();
        System.out.println("Notes:");
        System.out.println("- ordering=CORRECT means: write data, then publish ready");
        System.out.println("- ordering=WRONG means: publish ready, then write data (staleReads should be observable)");
        System.out.println("- useVolatile=false may still print 0/0 on some machines/JIT runs; that does not mean it's correct.");
    }

    /**
     * 执行一次发布模式迭代测试（普通状态类版本）
     * 创建读写线程，演示在没有volatile保证下的可见性问题
     */
    private static void runOnePublicationIteration(Config config, Result result, PlainState state) throws InterruptedException {
        Thread reader = new Thread(() -> {
            // 读线程自旋等待ready标志变为true
            while (!state.ready) {
                Thread.onSpinWait();  // 提示CPU进行自旋优化
            }
            // 读取数据并检查是否为期望值
            int observed = state.data;
            if (observed != EXPECTED) {
                result.staleRead = true;  // 记录过期读取
            }
        }, "reader-plain");
        reader.setDaemon(true);

        Thread writer = new Thread(() -> {
            if (config.ordering == Ordering.CORRECT) {
                // 正确顺序：先写数据，再设置就绪标志
                state.data = EXPECTED;
                state.ready = true;
            } else {
                // 错误顺序：先设置就绪标志，再写数据（可能导致过期读）
                state.ready = true;
                if (config.writerDelayMs > 0) {
                    // 延迟写数据，增加过期读的可能性
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(config.writerDelayMs));
                }
                state.data = EXPECTED;
            }
        }, "writer-plain");

        reader.start();
        writer.start();

        writer.join();  // 等待写线程完成
        reader.join(config.timeoutMs);  // 等待读线程完成，超时则强制继续
        if (reader.isAlive()) {
            result.timeout = true;  // 读线程超时未完成
        }
    }

    /**
     * 执行一次发布模式迭代测试（volatile状态类版本）
     * 创建读写线程，演示volatile保证下的正确行为
     */
    private static void runOnePublicationIteration(Config config, Result result, VolatileState state) throws InterruptedException {
        Thread reader = new Thread(() -> {
            // 读线程自旋等待ready标志变为true
            while (!state.ready) {
                Thread.onSpinWait();  // 提示CPU进行自旋优化
            }
            // 读取数据并检查是否为期望值
            int observed = state.data;
            if (observed != EXPECTED) {
                result.staleRead = true;  // 记录过期读取
            }
        }, "reader-volatile");
        reader.setDaemon(true);

        Thread writer = new Thread(() -> {
            if (config.ordering == Ordering.CORRECT) {
                // 正确顺序：先写数据，再设置就绪标志
                state.data = EXPECTED;
                state.ready = true;
            } else {
                // 错误顺序：先设置就绪标志，再写数据
                state.ready = true;
                if (config.writerDelayMs > 0) {
                    // 延迟写数据，增加观察到过期读的机会
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(config.writerDelayMs));
                }
                state.data = EXPECTED;
            }
        }, "writer-volatile");

        reader.start();
        writer.start();

        writer.join();  // 等待写线程完成
        reader.join(config.timeoutMs);  // 等待读线程完成，超时则强制继续
        if (reader.isAlive()) {
            result.timeout = true;  // 读线程超时未完成
        }
    }

    // 普通停止标志，没有volatile保证
    private static boolean plainStop = false;
    // volatile停止标志，保证可见性
    private static volatile boolean volatileStop = false;

    /**
     * 执行停止标志模式测试
     * 演示循环中使用停止标志的可见性问题
     */
    private static void runStopFlag(Config config, long startNs) throws InterruptedException {
        long stuck = 0;  // 循环卡住计数

        for (long i = 0; i < config.stopTrials; i++) {
            if (config.useVolatile) {
                // 使用volatile停止标志
                volatileStop = false;
                Thread reader = new Thread(() -> {
                    // 循环等待停止标志
                    while (!volatileStop) {
                        Thread.onSpinWait();  // 提示CPU进行自旋优化
                    }
                }, "stop-reader-volatile");
                reader.setDaemon(true);
                reader.start();

                // 短暂延迟后设置停止标志
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
                volatileStop = true;

                reader.join(config.timeoutMs);  // 等待读线程结束
                if (reader.isAlive()) {
                    stuck++;  // 读线程仍未结束，说明可能卡在循环中
                }
            } else {
                // 使用普通停止标志（无volatile）
                plainStop = false;
                Thread reader = new Thread(() -> {
                    // 循环等待停止标志
                    while (!plainStop) {
                        Thread.onSpinWait();  // 提示CPU进行自旋优化
                    }
                }, "stop-reader-plain");
                reader.setDaemon(true);
                reader.start();

                // 短暂延迟后设置停止标志
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(5));
                plainStop = true;

                reader.join(config.timeoutMs);  // 等待读线程结束
                if (reader.isAlive()) {
                    stuck++;  // 读线程仍未结束，说明可能卡在循环中
                }
            }
        }

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        System.out.println("== VolatileVisibilityDemo (mode=STOP_FLAG) ==");
        System.out.println("trials=" + config.stopTrials + ", timeoutMs=" + config.timeoutMs + ", useVolatile=" + config.useVolatile);
        System.out.println("stuck=" + stuck);
        System.out.println("elapsedMs=" + elapsedMs);
        System.out.println();
        System.out.println("Notes:");
        System.out.println("- useVolatile=false stuck may be 0 on some machines/JIT runs; it's still a data race and not guaranteed by JMM.");
        System.out.println("- If you want to increase the chance of observing stuck, try larger trials and timeout:");
        System.out.println("  java VolatileVisibilityDemo --mode stopFlag --useVolatile false --stopTrials 5000 --timeoutMs 200");
    }

    // 解析命令行参数
    private static Config parseArgs(String[] args) {
        Config config = new Config();
        if (args.length == 0) {
            return config;
        }

        if (args.length % 2 != 0) {
            usageAndExit();
        }

        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String key = args[i];
            if (!key.startsWith("--")) {
                usageAndExit();
            }
            values.put(key.substring(2), args[i + 1]);
        }

        if (values.containsKey("iterations")) {
            config.iterations = parseLong(values.get("iterations"));
        }
        if (values.containsKey("timeoutMs")) {
            config.timeoutMs = parseLong(values.get("timeoutMs"));
        }
        if (values.containsKey("useVolatile")) {
            config.useVolatile = Boolean.parseBoolean(values.get("useVolatile"));
        }
        if (values.containsKey("mode")) {
            config.mode = parseMode(values.get("mode"));
        }
        if (values.containsKey("ordering")) {
            config.ordering = parseOrdering(values.get("ordering"));
        }
        if (values.containsKey("writerDelayMs")) {
            config.writerDelayMs = parseLong(values.get("writerDelayMs"));
        }
        if (values.containsKey("stopTrials")) {
            config.stopTrials = parseLong(values.get("stopTrials"));
        }

        return config;
    }

    private static Mode parseMode(String value) {
        String v = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("PUBLICATION".equals(v)) {
            return Mode.PUBLICATION;
        }
        if ("STOPFLAG".equals(v) || "STOP_FLAG".equals(v)) {
            return Mode.STOP_FLAG;
        }
        usageAndExit();
        return Mode.PUBLICATION;
    }

    private static Ordering parseOrdering(String value) {
        String v = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if ("CORRECT".equals(v)) {
            return Ordering.CORRECT;
        }
        if ("WRONG".equals(v) || "WRONG_ORDER".equals(v)) {
            return Ordering.WRONG;
        }
        usageAndExit();
        return Ordering.CORRECT;
    }

    // 解析长整型值，支持下划线分隔符
    private static long parseLong(String value) {
        return Long.parseLong(value.replace("_", ""));
    }

    // 显示使用说明并退出
    private static void usageAndExit() {
        System.out.println("Usage:");
        System.out.println("  # publication (message passing) - default");
        System.out.println("  java VolatileVisibilityDemo --iterations 10000 --timeoutMs 50 --useVolatile true|false");
        System.out.println();
        System.out.println("  # force staleReads by using wrong ordering (publish ready first)");
        System.out.println("  java VolatileVisibilityDemo --mode publication --ordering wrong --writerDelayMs 1 --iterations 1000 --timeoutMs 50 --useVolatile true|false");
        System.out.println();
        System.out.println("  # stop flag visibility (may or may not reproduce on your machine)");
        System.out.println("  java VolatileVisibilityDemo --mode stopFlag --stopTrials 2000 --timeoutMs 200 --useVolatile true|false");
        System.exit(1);
    }
}
