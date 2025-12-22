import java.util.HashMap;
import java.util.Map;

public class StartJoinHappensBeforeDemo {
    private static final int EXPECTED = 42;
    private static int data;

    private static final class Config {
        long iterations = 1_000_000;
    }

    public static void main(String[] args) throws InterruptedException {
        Config config = parseArgs(args);
        long wrongReads = 0;
        long startNs = System.nanoTime();

        for (long i = 0; i < config.iterations; i++) {
            data = 0;
            Thread writer = new Thread(() -> data = EXPECTED);
            writer.start();
            writer.join();
            if (data != EXPECTED) {
                wrongReads++;
            }
        }

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        System.out.println("== StartJoinHappensBeforeDemo ==");
        System.out.println("iterations=" + config.iterations);
        System.out.println("wrongReads=" + wrongReads);
        System.out.println("elapsedMs=" + elapsedMs);
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

        return config;
    }

    private static long parseLong(String value) {
        return Long.parseLong(value.replace("_", ""));
    }

    private static void usageAndExit() {
        System.out.println("Usage: java StartJoinHappensBeforeDemo --iterations 1000000");
        System.exit(1);
    }
}
