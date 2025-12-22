import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadLocalLeakDemo {
    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 30;
        int mbPerValue = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        boolean remove = args.length > 2 && Boolean.parseBoolean(args[2]);

        ExecutorService pool = Executors.newSingleThreadExecutor();
        Runtime runtime = Runtime.getRuntime();

        for (int i = 1; i <= iterations; i++) {
            Future<?> future = pool.submit(() -> {
                ThreadLocal<byte[]> local = new ThreadLocal<>();
                local.set(new byte[mbPerValue * 1024 * 1024]);
                if (remove) {
                    local.remove();
                }
            });
            future.get();

            System.gc();
            Thread.sleep(50);
            long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
            System.out.println("iter=" + i + " remove=" + remove + " usedMB=" + usedMb);
        }

        pool.shutdown();
    }
}
