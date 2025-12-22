import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ScheduledThreadPoolExecutorFixedRateVsDelayDemo {
    private static final int RUNS = 5;
    private static final long PERIOD_MS = 100;
    private static final long WORK_MS = 200;

    public static void main(String[] args) throws Exception {
        System.out.println("fixed-rate:");
        runFixedRate();
        System.out.println();
        System.out.println("fixed-delay:");
        runFixedDelay();
    }

    private static void runFixedRate() throws Exception {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger();
        AtomicLong lastStart = new AtomicLong();
        ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        holder[0] = executor.scheduleAtFixedRate(() -> {
            int current = count.incrementAndGet();
            long now = System.nanoTime();
            long prev = lastStart.getAndSet(now);
            if (prev == 0L) {
                System.out.println("run " + current + " start");
            } else {
                long deltaMs = TimeUnit.NANOSECONDS.toMillis(now - prev);
                System.out.println("run " + current + " intervalMs=" + deltaMs);
            }
            sleepMillis(WORK_MS);
            if (current >= RUNS) {
                holder[0].cancel(false);
                latch.countDown();
            }
        }, 0, PERIOD_MS, TimeUnit.MILLISECONDS);
        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    private static void runFixedDelay() throws Exception {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger();
        AtomicLong lastStart = new AtomicLong();
        ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        holder[0] = executor.scheduleWithFixedDelay(() -> {
            int current = count.incrementAndGet();
            long now = System.nanoTime();
            long prev = lastStart.getAndSet(now);
            if (prev == 0L) {
                System.out.println("run " + current + " start");
            } else {
                long deltaMs = TimeUnit.NANOSECONDS.toMillis(now - prev);
                System.out.println("run " + current + " intervalMs=" + deltaMs);
            }
            sleepMillis(WORK_MS);
            if (current >= RUNS) {
                holder[0].cancel(false);
                latch.countDown();
            }
        }, 0, PERIOD_MS, TimeUnit.MILLISECONDS);
        latch.await();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    private static void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
