import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public class JoinSleepYieldDaemonDemo {
    public static void main(String[] args) throws Exception {
        String mode = argValue(args, "--mode", "join-timeout").toLowerCase(Locale.ROOT);
        System.out.println("== JoinSleepYieldDaemonDemo (JDK " + System.getProperty("java.version") + ") ==");
        System.out.println("mode=" + mode);
        System.out.println();

        switch (mode) {
            case "join-timeout":
                joinTimeoutDemo();
                break;
            case "yield":
                yieldDemo();
                break;
            case "daemon":
                daemonDemo(args);
                break;
            default:
                System.err.println("Unknown --mode: " + mode);
                System.err.println("Supported: --mode join-timeout | yield | daemon");
                System.exit(2);
        }
    }

    private static void joinTimeoutDemo() throws Exception {
        System.out.println("-- demo: join(timeout) usually shows TIMED_WAITING on the joiner --");
        Thread worker = new Thread(() -> sleepQuietly(800), "worker");

        Thread joiner = new Thread(() -> {
            Instant start = Instant.now();
            try {
                worker.join(1_500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Duration elapsed = Duration.between(start, Instant.now());
            System.out.println("  [joiner] join returned; workerAlive=" + worker.isAlive() + ", elapsedMs=" + elapsed.toMillis());
        }, "joiner");

        worker.start();
        joiner.start();

        Instant deadline = Instant.now().plusMillis(1_200);
        Thread.State lastJoiner = null;
        Thread.State lastWorker = null;
        while (Instant.now().isBefore(deadline) && joiner.isAlive()) {
            Thread.State joinerState = joiner.getState();
            Thread.State workerState = worker.getState();
            if (joinerState != lastJoiner || workerState != lastWorker) {
                System.out.println("  [main] joinerState=" + joinerState + ", workerState=" + workerState);
                lastJoiner = joinerState;
                lastWorker = workerState;
            }
            Thread.sleep(30);
        }

        worker.join();
        joiner.join();
        System.out.println("  [main] done");
    }

    private static void yieldDemo() throws Exception {
        System.out.println("-- demo: Thread.yield() is a hint; results are non-deterministic --");
        Duration duration = Duration.ofMillis(300);
        CounterThread a = new CounterThread("A", true, duration);
        CounterThread b = new CounterThread("B", false, duration);

        a.start();
        b.start();
        a.join();
        b.join();

        System.out.println("  A(yield) iterations=" + a.iterations);
        System.out.println("  B(no-yield) iterations=" + b.iterations);
        System.out.println("  Note: Run multiple times to see variability.");
    }

    private static void daemonDemo(String[] args) throws Exception {
        boolean daemon = Boolean.parseBoolean(argValue(args, "--daemon", "true"));
        long mainSleepMs = Long.parseLong(argValue(args, "--mainSleepMs", "250"));
        long heartbeatMs = Long.parseLong(argValue(args, "--heartbeatMs", "80"));

        System.out.println("-- demo: daemon thread does not keep JVM alive --");
        System.out.println("  daemon=" + daemon + ", mainSleepMs=" + mainSleepMs + ", heartbeatMs=" + heartbeatMs);

        Thread t = new Thread(() -> {
            long n = 0;
            while (true) {
                System.out.println("  [bg] heartbeat " + (++n));
                sleepQuietly(heartbeatMs);
            }
        }, "bg-heartbeat");
        t.setDaemon(daemon);
        t.start();

        Thread.sleep(mainSleepMs);
        System.out.println("  [main] exiting now (if daemon=true, process ends immediately)");
    }

    private static final class CounterThread extends Thread {
        private final boolean doYield;
        private final Duration duration;
        long iterations;

        private CounterThread(String name, boolean doYield, Duration duration) {
            super(name);
            this.doYield = doYield;
            this.duration = duration;
        }

        @Override
        public void run() {
            // 计算截止时间点，用于控制线程运行时长
            long deadline = System.nanoTime() + duration.toNanos();
            
            // 在截止时间前持续循环计数
            while (System.nanoTime() < deadline) {
                iterations++; // 增加迭代计数
                
                // 每1000次迭代检查一次是否需要让出CPU
                if (doYield && (iterations % 1_000) == 0) {
                    Thread.yield(); // 让出CPU执行权，给其他线程机会
                }
            }
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String argValue(String[] args, String key, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}
