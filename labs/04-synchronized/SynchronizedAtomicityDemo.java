import java.util.concurrent.CountDownLatch;

public class SynchronizedAtomicityDemo {
    private static final class Counter {
        private int value;

        void inc() {
            value++;
        }

        synchronized void incSync() {
            value++;
        }

        int get() {
            return value;
        }
    }

    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int loops = args.length > 1 ? Integer.parseInt(args[1]) : 1_000_000;

        runTest("no_sync", threads, loops, false);
        runTest("sync", threads, loops, true);
    }

    private static void runTest(String label, int threads, int loops, boolean sync) throws Exception {
        Counter counter = new Counter();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < loops; j++) {
                        if (sync) {
                            counter.incSync();
                        } else {
                            counter.inc();
                        }
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
        long end = System.nanoTime();

        int expected = threads * loops;
        int actual = counter.get();
        long ms = (end - begin) / 1_000_000;

        System.out.println(label + " threads=" + threads + " loops=" + loops +
                " expected=" + expected + " actual=" + actual + " ms=" + ms);
    }
}
