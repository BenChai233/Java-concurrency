import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

public class FairVsNonfairLockDemo {
    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 6;

        runTest("fair", new ReentrantLock(true), threads);
        runTest("nonfair", new ReentrantLock(false), threads);
    }

    private static void runTest(String label, ReentrantLock lock, int threads) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Integer> order = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    lock.lock();
                    try {
                        order.add(id);
                        Thread.sleep(50);
                    } finally {
                        lock.unlock();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, label + "-t" + id);
            t.start();
        }

        start.countDown();
        done.await();
        System.out.println(label + " order=" + order);
    }
}
