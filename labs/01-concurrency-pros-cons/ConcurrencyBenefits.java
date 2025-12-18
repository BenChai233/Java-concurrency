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

    /**
     * 主方法：解析命令行参数，执行顺序和并发任务，并输出性能对比结果。
     * 
     * 执行流程：
     * 1. 解析命令行参数生成配置对象。
     * 2. 输出当前运行配置信息。
     * 3. 执行预热轮次（如果配置了的话）。
     * 4. 分别测量顺序执行和并发执行的时间。
     * 5. 输出两种执行方式的耗时及加速比。
     *
     * @param args 命令行参数
     * @throws Exception 如果在任务执行过程中发生异常
     */
    public static void main(String[] args) throws Exception {
        // 解析命令行参数获取配置
        Config config = Config.fromArgs(args);
        
        // 打印当前运行配置信息
        System.out.printf(Locale.ROOT,
                "mode=%s tasks=%d threads=%d virtual=%s work=%d sleepMs=%d warmup=%d%n",
                config.mode, config.tasks, config.threads, config.useVirtualThreads,
                config.work, config.sleepMs, config.warmupRounds);

        // 预热 JVM 以获得更准确的性能测试结果
        warmup(config);

        // 测量顺序执行时间
        long sequentialNanos = timeSequential(config);
        
        // 测量并发执行时间
        long concurrentNanos = timeConcurrent(config);

        // 输出顺序执行时间
        System.out.printf(Locale.ROOT, "sequential=%s%n", fmt(sequentialNanos));
        
        // 输出并发执行时间
        System.out.printf(Locale.ROOT, "concurrent=%s%n", fmt(concurrentNanos));
        
        // 计算并输出加速比
        System.out.printf(Locale.ROOT, "speedup=%.2fx%n", (double) sequentialNanos / (double) concurrentNanos);
    }

    private static void warmup(Config config) throws Exception {
        if (config.warmupRounds <= 0) return;
        for (int i = 0; i < config.warmupRounds; i++) {
            timeSequential(config);
            timeConcurrent(config);
        }
    }


    /**
     * 测量顺序执行任务所需的时间。
     * 
     * 执行流程：
     * 1. 记录开始时间。
     * 2. 依次执行所有任务，并累计校验和。
     * 3. 计算并返回总耗时。
     * 4. 输出校验和以验证计算正确性。
     *
     * @param config 配置对象，包含任务数量等运行参数
     * @return 顺序执行所有任务所需的纳秒数
     * @throws Exception 如果在任务执行过程中发生异常
     */
    private static long timeSequential(Config config) throws Exception {
        // 记录顺序执行开始时间
        long start = System.nanoTime();
        
        // 初始化校验和
        long checksum = 0;
        
        // 依次执行每个任务，并将结果累加到校验和中
        for (int i = 0; i < config.tasks; i++) {
            checksum += doWork(config, i);
        }
        
        // 计算顺序执行总耗时
        long elapsed = System.nanoTime() - start;
        
        // 输出顺序执行的校验和
        System.out.printf(Locale.ROOT, "sequential checksum=%d%n", checksum);
        
        // 返回顺序执行耗时
        return elapsed;
    }


    /**
     * 测量并发执行任务所需的时间。
     * 
     * 执行流程：
     * 1. 根据配置决定使用虚拟线程还是固定线程池创建ExecutorService。
     * 2. 创建与任务数相等的Callable任务列表。
     * 3. 记录开始时间，提交所有任务并等待它们完成。
     * 4. 收集所有任务的结果并计算校验和。
     * 5. 计算并返回总耗时。
     *
     * @param config 配置对象，包含运行参数如线程数、是否使用虚拟线程等
     * @return 并发执行所有任务所需的纳秒数
     * @throws Exception 如果在任务执行过程中发生异常
     */
    private static long timeConcurrent(Config config) throws Exception {
        // 根据配置选择创建虚拟线程或固定大小线程池的执行器
        ExecutorService executor = config.useVirtualThreads
                ? Executors.newVirtualThreadPerTaskExecutor()
                : Executors.newFixedThreadPool(config.threads);

        // 使用try-with-resources确保executor在使用完毕后能正确关闭
        try (executor) {
            // 初始化任务列表，容量为任务总数
            List<Callable<Long>> callables = new ArrayList<>(config.tasks);
            
            // 为每个任务创建一个Callable实例，并添加到列表中
            for (int i = 0; i < config.tasks; i++) {
                final int id = i;
                callables.add(() -> doWork(config, id));
            }

            // 记录并发执行开始时间
            long start = System.nanoTime();
            
            // 提交所有任务并等待它们全部完成
            List<Future<Long>> futures = executor.invokeAll(callables);
            
            // 初始化校验和
            long checksum = 0;
            
            // 遍历所有Future对象，获取结果并累加到校验和中
            for (Future<Long> future : futures) {
                checksum += future.get();
            }
            
            // 计算并发执行总耗时
            long elapsed = System.nanoTime() - start;
            
            // 输出并发执行的校验和
            System.out.printf(Locale.ROOT, "concurrent checksum=%d%n", checksum);
            
            // 返回并发执行耗时
            return elapsed;
        }
    }


    /**
     * 根据配置的模式执行相应的工作任务
     * 
     * @param config 配置对象，包含运行模式和其他相关参数
     * @param id 任务ID，用于区分不同任务
     * @return 工作结果，用于校验计算正确性
     * @throws Exception 如果在执行工作任务时发生异常
     */
    private static long doWork(Config config, int id) throws Exception {
        return switch (config.mode) {
            // CPU密集型：执行指定迭代次数的计算任务
            case CPU -> cpuWork(config.work, id);
            // IO密集型：执行指定时间的睡眠模拟IO等待
            case IO -> ioWork(config.sleepMs, id);
            // 混合型：执行少量计算任务和睡眠等待的组合
            case MIX -> cpuWork(config.work / 5, id) + ioWork(config.sleepMs, id);
        };
    }

    private static long ioWork(int sleepMs, int id) throws InterruptedException {
        if (sleepMs > 0) Thread.sleep(sleepMs);
        return id;
    }

    /**
     * 执行CPU密集型工作，通过异或运算和位移操作模拟计算密集任务
     * 
     * @param iterations 迭代次数，控制计算量大小
     * @param seed 初始种子值，用于生成不同的计算序列
     * @return 计算结果，用于校验计算正确性
     */
    private static long cpuWork(long iterations, int seed) {

        // 使用幻数0x9E3779B97F4A7C15L（来自TEA算法的黄金比例）与种子值进行异或操作，
        // 为每个任务生成不同的初始状态，确保计算结果的唯一性和可校验性
        long x = 0x9E3779B97F4A7C15L ^ seed;

        // 循环执行指定次数的位运算操作，模拟CPU密集型计算任务
        // 每次迭代都会对x进行左移、无符号右移和异或操作，增加计算复杂度
        for (long i = 0; i < iterations; i++) {
            // 左移13位后与原值异或，扩散高位的变化
            x ^= (x << 13);
            // 无符号右移7位后与原值异或，使低位也参与变化
            x ^= (x >>> 7);
            // 左移17位后与原值异或，进一步混合数据
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

    /**
     * 配置类，用于存储程序运行的各种参数。
     * 包括运行模式、任务数量、线程数、是否使用虚拟线程、工作量、睡眠时间和预热轮次等。
     */
    static final class Config {
        /** 运行模式：CPU密集型、IO密集型或混合模式 */
        final Mode mode;
        /** 任务总数 */
        final int tasks;
        /** 并发线程数 */
        final int threads;
        /** 是否启用虚拟线程 */
        final boolean useVirtualThreads;
        /** CPU工作量（迭代次数）*/
        final long work;
        /** IO等待时间（毫秒）*/
        final int sleepMs;
        /** 预热轮次，用于JVM预热以获得更准确的性能数据 */
        final int warmupRounds;

        /**
         * 私有构造函数，防止外部直接实例化
         * @param mode 运行模式
         * @param tasks 任务数量
         * @param threads 线程数
         * @param useVirtualThreads 是否使用虚拟线程
         * @param work 工作量
         * @param sleepMs 睡眠时间
         * @param warmupRounds 预热轮次
         */
        private Config(Mode mode, int tasks, int threads, boolean useVirtualThreads, long work, int sleepMs, int warmupRounds) {
            this.mode = mode;
            this.tasks = tasks;
            this.threads = threads;
            this.useVirtualThreads = useVirtualThreads;
            this.work = work;
            this.sleepMs = sleepMs;
            this.warmupRounds = warmupRounds;
        }

        /**
         * 从命令行参数解析配置
         * @param args 命令行参数数组
         * @return 解析后的Config对象
         */
        static Config fromArgs(String[] args) {
            // 设置默认值
            Mode mode = Mode.IO;
            int tasks = 200;
            int threads = Math.max(2, Runtime.getRuntime().availableProcessors());
            boolean virtual = false;
            long work = 4_000_000L;
            int sleepMs = 20;
            int warmup = 1;

            // 解析命令行参数
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
                        // 忽略未知标志以保持演示轻量级
                    }
                }
            }

            // 参数验证
            if (tasks <= 0) throw new IllegalArgumentException("--tasks must be > 0");
            if (threads <= 0) throw new IllegalArgumentException("--threads must be > 0");
            if (work < 0) throw new IllegalArgumentException("--work must be >= 0");
            if (sleepMs < 0) throw new IllegalArgumentException("--sleepMs must be >= 0");
            if (warmup < 0) throw new IllegalArgumentException("--warmup must be >= 0");

            return new Config(mode, tasks, threads, virtual, work, sleepMs, warmup);
        }

        /**
         * 检查命令行参数是否有对应的值
         * @param flag 参数标志
         * @param value 参数值
         * @return 如果值有效则返回该值，否则抛出异常
         */
        private static String requireValue(String flag, String value) {
            if (value == null || value.startsWith("--")) throw new IllegalArgumentException("Missing value for " + flag);
            return value;
        }
    }
}

