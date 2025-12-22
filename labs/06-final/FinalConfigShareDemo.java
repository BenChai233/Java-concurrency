public class FinalConfigShareDemo {
    private static final class Config {
        final String name;
        final int size;
        final boolean enabled;

        Config(String name, int size, boolean enabled) {
            this.name = name;
            this.size = size;
            this.enabled = enabled;
        }
    }

    private static final Config CONFIG = new Config("demo", 42, true);

    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 4;

        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            int id = i;
            workers[i] = new Thread(() -> {
                String snapshot = CONFIG.name + ":" + CONFIG.size + ":" + CONFIG.enabled;
                System.out.println("t" + id + " read=" + snapshot);
            }, "t-" + i);
            workers[i].start();
        }

        for (Thread t : workers) {
            t.join();
        }
    }
}
