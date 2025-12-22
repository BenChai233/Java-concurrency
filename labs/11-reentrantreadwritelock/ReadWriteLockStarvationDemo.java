import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockStarvationDemo {
    public static void main(String[] args) throws Exception {
        int readers = args.length > 0 ? Integer.parseInt(args[0]) : 6;
        int holdMs = args.length > 1 ? Integer.parseInt(args[1]) : 50;
        int maxWaitMs = args.length > 2 ? Integer.parseInt(args[2]) : 2000;

        run(false, readers, holdMs, maxWaitMs);
        run(true, readers, holdMs, maxWaitMs);
    }

    private static void run(boolean fair, int readers, int holdMs, int maxWaitMs) throws Exception {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(fair);
        Lock readLock = rwLock.readLock();
        Lock writeLock = rwLock.writeLock();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch firstRead = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean(false);

        Thread[] readerThreads = new Thread[readers];
        for (int i = 0; i < readers; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    while (!stop.get()) {
                        readLock.lock();
                        try {
                            firstRead.countDown();
                            TimeUnit.MILLISECONDS.sleep(holdMs);
                        } finally {
                            readLock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "reader-" + i);
            readerThreads[i] = t;
            t.start();
        }

        start.countDown();
        firstRead.await();

        long begin = System.nanoTime();
        boolean acquired = writeLock.tryLock(maxWaitMs, TimeUnit.MILLISECONDS);
        long waitMs = (System.nanoTime() - begin) / 1_000_000;
        if (acquired) {
            writeLock.unlock();
        }
        stop.set(true);
        for (Thread t : readerThreads) {
            t.join();
        }

        System.out.println("fair=" + fair + " readers=" + readers + " holdMs=" + holdMs
                + " writerAcquired=" + acquired + " writerWaitMs=" + waitMs);
    }
}
