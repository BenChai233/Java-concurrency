import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CountDownLatchDemo {
    public static void main(String[] args) throws Exception {
        int workers = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        CountDownLatch latch = new CountDownLatch(workers);

        for (int i = 0; i < workers; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                System.out.println("worker-" + id + " start");
                sleepMillis(200 + id * 100L);
                System.out.println("worker-" + id + " done");
                latch.countDown();
            }, "worker-" + id);
            t.start();
        }

        long start = System.nanoTime();
        latch.await();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        System.out.println("all workers done, waited " + elapsedMs + " ms");
    }

    private static void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
