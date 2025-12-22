import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockReadConcurrencyDemo {
    public static void main(String[] args) throws Exception {
        int readers = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int holdMs = args.length > 1 ? Integer.parseInt(args[1]) : 200;

        run("readLock", readers, holdMs, true);
        run("writeLock", readers, holdMs, false);
    }

    private static void run(String label, int readers, int holdMs, boolean useReadLock) throws Exception {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        Lock lock = useReadLock ? rwLock.readLock() : rwLock.writeLock();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(readers);

        for (int i = 0; i < readers; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    lock.lock();
                    try {
                        TimeUnit.MILLISECONDS.sleep(holdMs);
                    } finally {
                        lock.unlock();
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

        System.out.println(label + " readers=" + readers + " holdMs=" + holdMs + " elapsedMs=" + ms);
    }
}
