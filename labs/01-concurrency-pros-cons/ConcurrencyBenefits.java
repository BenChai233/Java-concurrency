import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 第 1 章实验 A：并发收益边界（CPU 密集 vs 等待密集）。
 *
 * 观察重点：
 * - CPU 密集：线程数超过 CPU 核心数后，收益下降甚至变慢（调度/切换/缓存压力）。
 * - 等待密集（sleep/IO 模拟）：并发可以隐藏等待；JDK21 虚拟线程通常更适合“很多阻塞任务”。
 *
 * 运行示例：
 *   javac --release 21 ConcurrencyBenefits.java
 *   java ConcurrencyBenefits --mode cpu --tasks 200 --threads 8 --work 4000000
 *   java ConcurrencyBenefits --mode io  --tasks 200 --threads 8 --sleepMs 20
 *   java ConcurrencyBenefits --mode io  --tasks 200 --virtual true --sleepMs 20
 */
public class ConcurrencyBenefits {
    enum Mode { CPU, IO, MIX }

    public static void main(String[] args) throws Exception {
        Config config = Config.fromArgs(args);
        System.out.printf(Locale.ROOT,
                "mode=%s tasks=%d threads=%d virtual=%s work=%d sleepMs=%d warmup=%d%n",
                config.mode, config.tasks, config.threads, config.useVirtualThreads,
                config.work, config.sleepMs, config.warmupRounds);

        warmup(config);

        long sequentialNanos = timeSequential(config);
        long concurrentNanos = timeConcurrent(config);

        System.out.printf(Locale.ROOT, "sequential=%s%n", fmt(sequentialNanos));
        System.out.printf(Locale.ROOT, "concurrent=%s%n", fmt(concurrentNanos));
        System.out.printf(Locale.ROOT, "speedup=%.2fx%n", (double) sequentialNanos / (double) concurrentNanos);
    }

    private static void warmup(Config config) throws Exception {
        if (config.warmupRounds <= 0) return;
        for (int i = 0; i < config.warmupRounds; i++) {
            timeSequential(config);
            timeConcurrent(config);
        }
    }

    private static long timeSequential(Config config) throws Exception {
        long start = System.nanoTime();
        long checksum = 0;
        for (int i = 0; i < config.tasks; i++) {
            checksum += doWork(config, i);
        }
        long elapsed = System.nanoTime() - start;
        System.out.printf(Locale.ROOT, "sequential checksum=%d%n", checksum);
        return elapsed;
    }

    private static long timeConcurrent(Config config) throws Exception {
        ExecutorService executor = config.useVirtualThreads
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(config.threads);

        try (executor) {
            List<Callable<Long>> callables = new ArrayList<>(config.tasks);
            for (int i = 0; i < config.tasks; i++) {
                final int id = i;
                callables.add(() -> doWork(config, id));
            }

            long start = System.nanoTime();
            List<Future<Long>> futures = executor.invokeAll(callables);
            long checksum = 0;
            for (Future<Long> future : futures) {
                checksum += future.get();
            }
            long elapsed = System.nanoTime() - start;
            System.out.printf(Locale.ROOT, "concurrent checksum=%d%n", checksum);
            return elapsed;
        }
    }

    private static long doWork(Config config, int id) throws Exception {
        return switch (config.mode) {
            case CPU -> cpuWork(config.work, id);
            case IO -> ioWork(config.sleepMs, id);
            case MIX -> cpuWork(config.work / 5, id) + ioWork(config.sleepMs, id);
        };
    }

    private static long ioWork(int sleepMs, int id) throws InterruptedException {
        if (sleepMs > 0) Thread.sleep(sleepMs);
        return id;
    }

    private static long cpuWork(long iterations, int seed) {
        long x = 0x9E3779B97F4A7C15L ^ seed;
        for (long i = 0; i < iterations; i++) {
            x ^= (x << 13);
            x ^= (x >>> 7);
            x ^= (x << 17);
        }
        return x;
    }

    private static String fmt(long nanos) {
        Duration d = Duration.ofNanos(nanos);
        long ms = d.toMillis();
        if (ms < 1_000) return ms + "ms";
        return String.format(Locale.ROOT, "%.2fs", ms / 1000.0);
    }

    static final class Config {
        final Mode mode;
        final int tasks;
        final int threads;
        final boolean useVirtualThreads;
        final long work;
        final int sleepMs;
        final int warmupRounds;

        private Config(Mode mode, int tasks, int threads, boolean useVirtualThreads, long work, int sleepMs, int warmupRounds) {
            this.mode = mode;
            this.tasks = tasks;
            this.threads = threads;
            this.useVirtualThreads = useVirtualThreads;
            this.work = work;
            this.sleepMs = sleepMs;
            this.warmupRounds = warmupRounds;
        }

        static Config fromArgs(String[] args) {
            Mode mode = Mode.IO;
            int tasks = 200;
            int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
            boolean virtual = false;
            long work = 4_000_000L;
            int sleepMs = 20;
            int warmup = 1;

            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                String v = (i + 1 < args.length) ? args[i + 1] : null;

                switch (a) {
                    case "--mode" -> {
                        mode = Mode.valueOf(requireValue(a, v).toUpperCase(Locale.ROOT));
                        i++;
                    }
                    case "--tasks" -> {
                        tasks = Integer.parseInt(requireValue(a, v).replace("_", ""));
                        i++;
                    }
                    case "--threads" -> {
                        threads = Integer.parseInt(requireValue(a, v).replace("_", ""));
                        i++;
                    }
                    case "--virtual" -> {
                        virtual = Boolean.parseBoolean(requireValue(a, v));
                        i++;
                    }
                    case "--work" -> {
                        work = Long.parseLong(requireValue(a, v).replace("_", ""));
                        i++;
                    }
                    case "--sleepMs" -> {
                        sleepMs = Integer.parseInt(requireValue(a, v).replace("_", ""));
                        i++;
                    }
                    case "--warmup" -> {
                        warmup = Integer.parseInt(requireValue(a, v).replace("_", ""));
                        i++;
                    }
                    default -> {
                        // ignore unknown flags to keep the demo lightweight
                    }
                }
            }

            if (tasks <= 0) throw new IllegalArgumentException("--tasks must be > 0");
            if (threads <= 0) throw new IllegalArgumentException("--threads must be > 0");
            if (work < 0) throw new IllegalArgumentException("--work must be >= 0");
            if (sleepMs < 0) throw new IllegalArgumentException("--sleepMs must be >= 0");
            if (warmup < 0) throw new IllegalArgumentException("--warmup must be >= 0");

            return new Config(mode, tasks, threads, virtual, work, sleepMs, warmup);
        }

        private static String requireValue(String flag, String value) {
            if (value == null || value.startsWith("--")) throw new IllegalArgumentException("Missing value for " + flag);
            return value;
        }
    }
}

