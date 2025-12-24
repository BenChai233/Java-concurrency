import java.util.HashMap;
import java.util.Map;
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

    // 配置类，用于控制测试参数
    private static final class Config {
        long iterations = 10_000;  // 迭代次数
        long timeoutMs = 50;  // 超时时间（毫秒）
        boolean useVolatile = true;  // 是否使用volatile
    }

    public static void main(String[] args) throws InterruptedException {
        // 解析命令行参数
        Config config = parseArgs(args);
        long startNs = System.nanoTime();
        long timeouts = 0;
        long staleReads = 0;

        // 执行指定次数的测试迭代
        for (long i = 0; i < config.iterations; i++) {
            Result result = new Result();
            Thread reader;
            Thread writer;
            
            // 根据配置选择使用volatile或普通变量的版本
            if (config.useVolatile) {
                VolatileState state = new VolatileState();
                // 创建读线程，使用Lambda表达式传递状态访问逻辑
                reader = new Thread(() -> readWithTimeout(() -> state.ready, () -> state.data, config, result));
                // 创建写线程，设置数据后标记就绪
                writer = new Thread(() -> {
                    state.data = EXPECTED;
                    state.ready = true;
                });

            } else {
                PlainState state = new PlainState();
                // 创建读线程，使用Lambda表达式传递状态访问逻辑
                reader = new Thread(() -> readWithTimeout(() -> state.ready, () -> state.data, config, result));
                // 创建写线程，设置数据后标记就绪
                writer = new Thread(() -> {
                    state.data = EXPECTED;
                    state.ready = true;
                });
            }
            // 启动读写线程
            reader.start();
            writer.start();
            // 等待两个线程完成
            reader.join();
            writer.join();

            // 统计结果
            if (result.timeout) {
                timeouts++;
            } else if (result.staleRead) {
                staleReads++;
            }
        }

        // 计算并输出测试结果
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        System.out.println("== VolatileVisibilityDemo ==");
        System.out.println("iterations=" + config.iterations + ", timeoutMs=" + config.timeoutMs + ", useVolatile=" + config.useVolatile);
        System.out.println("timeouts=" + timeouts + ", staleReads=" + staleReads);
        System.out.println("elapsedMs=" + elapsedMs);
    }

    // 函数式接口，用于提供布尔值
    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    // 函数式接口，用于提供整数值
    @FunctionalInterface
    private interface IntSupplier {
        int getAsInt();
    }

    /**
     * 带超时的读取操作
     * 等待ready条件变为true，然后读取data值，检查是否为期望值
     * 
     * @param ready 用于检查是否就绪的布尔值提供器
     * @param data 用于获取数据值的整数提供器  
     * @param config 配置参数，包含超时时间等
     * @param result 结果对象，用于记录超时和过期读取情况
     */
    private static void readWithTimeout(BooleanSupplier ready, IntSupplier data, Config config, Result result) {
        // 计算超时截止时间
        long deadlineNs = System.nanoTime() + config.timeoutMs * 1_000_000;
        int spins = 0;
        
        // 自旋等待直到ready条件满足或超时
        while (!ready.getAsBoolean()) {
            // 检查是否超时
            if (System.nanoTime() >= deadlineNs) {
                result.timeout = true;
                return;
            }
            // 执行自旋等待，每256次自旋调用一次parkNanos，否则使用onSpinWait优化
            if ((++spins & 0xFF) == 0) {
                LockSupport.parkNanos(1_000_000); // 每256次自旋后短暂休眠1毫秒
            } else {
                Thread.onSpinWait(); // 优化自旋等待，提示CPU当前线程正在自旋
            }
        }

        // ready条件满足后，读取数据值
        int observed = data.getAsInt();
        // 检查读取到的值是否为期望值，如果不是则标记为过期读取
        if (observed != EXPECTED) {
            result.staleRead = true;
        }
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

        return config;
    }

    // 解析长整型值，支持下划线分隔符
    private static long parseLong(String value) {
        return Long.parseLong(value.replace("_", ""));
    }

    // 显示使用说明并退出
    private static void usageAndExit() {
        System.out.println("Usage: java VolatileVisibilityDemo --iterations 10000 --timeoutMs 50 --useVolatile true|false");
        System.exit(1);
    }
}
