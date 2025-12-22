import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class SimpleAqsMutexDemo {
    private static final class Mutex {
        private final Sync sync = new Sync();

        private static final class Sync extends AbstractQueuedSynchronizer {
            @Override
            protected boolean tryAcquire(int acquires) {
                return compareAndSetState(0, 1);
            }

            @Override
            protected boolean tryRelease(int releases) {
                setState(0);
                return true;
            }

            boolean isLocked() {
                return getState() == 1;
            }
        }

        void lock() {
            sync.acquire(1);
        }

        void unlock() {
            sync.release(1);
        }

        boolean isLocked() {
            return sync.isLocked();
        }
    }

    private static int counter = 0;

    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int loops = args.length > 1 ? Integer.parseInt(args[1]) : 200_000;

        Mutex mutex = new Mutex();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < loops; j++) {
                        mutex.lock();
                        try {
                            counter++;
                        } finally {
                            mutex.unlock();
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
        long ms = (System.nanoTime() - begin) / 1_000_000;

        int expected = threads * loops;
        System.out.println("expected=" + expected + " actual=" + counter + " ms=" + ms);
        System.out.println("mutex locked=" + mutex.isLocked());
    }
}
