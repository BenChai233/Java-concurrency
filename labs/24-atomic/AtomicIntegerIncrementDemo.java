import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerIncrementDemo {
    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 200_000;

        int expected = threads * iterations;
        int[] nonAtomic = new int[1];
        AtomicInteger atomic = new AtomicInteger();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                await(start);
                for (int j = 0; j < iterations; j++) {
                    nonAtomic[0]++;
                    atomic.incrementAndGet();
                }
                done.countDown();
            }, "worker-" + i);
            t.start();
        }

        start.countDown();
        done.await();

        System.out.println("expected=" + expected);
        System.out.println("nonAtomic=" + nonAtomic[0]);
        System.out.println("atomic=" + atomic.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
