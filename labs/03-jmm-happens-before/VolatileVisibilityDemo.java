import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

public class VolatileVisibilityDemo {
    private static final int EXPECTED = 42;

    private static final class PlainState {
        int data;
        boolean ready;
    }

    private static final class VolatileState {
        int data;
        volatile boolean ready;
    }

    private static final class Result {
        boolean timeout;
        boolean staleRead;
    }

    private static final class Config {
        long iterations = 10_000;
        long timeoutMs = 50;
        boolean useVolatile = true;
    }

    public static void main(String[] args) throws InterruptedException {
        Config config = parseArgs(args);
        long startNs = System.nanoTime();
        long timeouts = 0;
        long staleReads = 0;

        for (long i = 0; i < config.iterations; i++) {
            Result result = new Result();
            if (config.useVolatile) {
                VolatileState state = new VolatileState();
                Thread reader = new Thread(() -> readWithTimeout(() -> state.ready, () -> state.data, config, result));
                Thread writer = new Thread(() -> {
                    state.data = EXPECTED;
                    state.ready = true;
                });
                reader.start();
                writer.start();
                reader.join();
                writer.join();
            } else {
                PlainState state = new PlainState();
                Thread reader = new Thread(() -> readWithTimeout(() -> state.ready, () -> state.data, config, result));
                Thread writer = new Thread(() -> {
                    state.data = EXPECTED;
                    state.ready = true;
                });
                reader.start();
                writer.start();
                reader.join();
                writer.join();
            }

            if (result.timeout) {
                timeouts++;
            } else if (result.staleRead) {
                staleReads++;
            }
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        System.out.println("== VolatileVisibilityDemo ==");
        System.out.println("iterations=" + config.iterations + ", timeoutMs=" + config.timeoutMs + ", useVolatile=" + config.useVolatile);
        System.out.println("timeouts=" + timeouts + ", staleReads=" + staleReads);
        System.out.println("elapsedMs=" + elapsedMs);
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    @FunctionalInterface
    private interface IntSupplier {
        int getAsInt();
    }

    private static void readWithTimeout(BooleanSupplier ready, IntSupplier data, Config config, Result result) {
        long deadlineNs = System.nanoTime() + config.timeoutMs * 1_000_000;
        int spins = 0;
        while (!ready.getAsBoolean()) {
            if (System.nanoTime() >= deadlineNs) {
                result.timeout = true;
                return;
            }
            if ((++spins & 0xFF) == 0) {
                LockSupport.parkNanos(1_000_000);
            } else {
                Thread.onSpinWait();
            }
        }

        int observed = data.getAsInt();
        if (observed != EXPECTED) {
            result.staleRead = true;
        }
    }

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

    private static long parseLong(String value) {
        return Long.parseLong(value.replace("_", ""));
    }

    private static void usageAndExit() {
        System.out.println("Usage: java VolatileVisibilityDemo --iterations 10000 --timeoutMs 50 --useVolatile true|false");
        System.exit(1);
    }
}
