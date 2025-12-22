import java.util.concurrent.CountDownLatch;

public class AtomicitySummaryDemo {
    private static int counter = 0;

    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int loops = args.length > 1 ? Integer.parseInt(args[1]) : 1_000_000;

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < loops; j++) {
                        counter++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "t-" + i);
            t.start();
        }

        long begin = System.nanoTime();
        start.countDown();
        done.await();
        long ms = (System.nanoTime() - begin) / 1_000_000;

        int expected = threads * loops;
        System.out.println("threads=" + threads + " loops=" + loops +
                " expected=" + expected + " actual=" + counter + " ms=" + ms);
    }
}
