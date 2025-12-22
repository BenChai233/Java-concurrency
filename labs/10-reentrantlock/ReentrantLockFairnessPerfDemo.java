import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockFairnessPerfDemo {
    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int loops = args.length > 1 ? Integer.parseInt(args[1]) : 500_000;

        runTest("fair", new ReentrantLock(true), threads, loops);
        runTest("nonfair", new ReentrantLock(false), threads, loops);
    }

    private static void runTest(String label, ReentrantLock lock, int threads, int loops) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        int[] counter = new int[1];

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < loops; j++) {
                        lock.lock();
                        try {
                            counter[0]++;
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, label + "-t" + i);
            t.start();
        }

        long begin = System.nanoTime();
        start.countDown();
        done.await();
        long ms = (System.nanoTime() - begin) / 1_000_000;

        int expected = threads * loops;
        System.out.println(label + " expected=" + expected +
                " actual=" + counter[0] + " ms=" + ms);
    }
}
