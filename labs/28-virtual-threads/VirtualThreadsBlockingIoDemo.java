import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualThreadsBlockingIoDemo {
    private static long run(ExecutorService executor, int tasks, int sleepMs) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(tasks);
        long start = System.nanoTime();
        for (int i = 0; i < tasks; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private static void shutdownAndAwait(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    public static void main(String[] args) throws Exception {
        int tasks = args.length > 0 ? Integer.parseInt(args[0]) : 2000;
        int sleepMs = args.length > 1 ? Integer.parseInt(args[1]) : 50;
        int poolSize = args.length > 2 ? Integer.parseInt(args[2]) : 64;

        ExecutorService platform = Executors.newFixedThreadPool(poolSize);
        long platformMs;
        try {
            platformMs = run(platform, tasks, sleepMs);
        } finally {
            shutdownAndAwait(platform);
        }

        ExecutorService virtualExec = Executors.newVirtualThreadPerTaskExecutor();
        long virtualMs;
        try {
            virtualMs = run(virtualExec, tasks, sleepMs);
        } finally {
            shutdownAndAwait(virtualExec);
        }

        System.out.println("tasks=" + tasks + " sleepMs=" + sleepMs + " poolSize=" + poolSize);
        System.out.println("platform elapsedMs=" + platformMs);
        System.out.println("virtual elapsedMs=" + virtualMs);
    }
}
