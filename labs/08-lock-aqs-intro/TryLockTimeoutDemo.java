import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TryLockTimeoutDemo {
    private static final ReentrantLock LOCK = new ReentrantLock();

    public static void main(String[] args) throws Exception {
        long holdMs = args.length > 0 ? Long.parseLong(args[0]) : 500;
        long timeoutMs = args.length > 1 ? Long.parseLong(args[1]) : 200;

        Thread holder = new Thread(() -> {
            LOCK.lock();
            try {
                sleep(holdMs);
            } finally {
                LOCK.unlock();
            }
        }, "holder");

        Thread tryer = new Thread(() -> {
            long begin = System.nanoTime();
            boolean acquired = false;
            try {
                acquired = LOCK.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (acquired) {
                    LOCK.unlock();
                }
            }
            long ms = (System.nanoTime() - begin) / 1_000_000;
            System.out.println("tryLock acquired=" + acquired + " wait_ms=" + ms);
        }, "tryer");

        holder.start();
        Thread.sleep(50);
        tryer.start();
        holder.join();
        tryer.join();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
