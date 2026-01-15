import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockInterruptDemo {
    private static final ReentrantLock LOCK = new ReentrantLock();

    public static void main(String[] args) throws Exception {
        Thread holder = new Thread(() -> {
            LOCK.lock();
            try {
                sleep(1000);
            } finally {
                System.out.println("holder unlock");
                LOCK.unlock();
            }
        }, "holder");

        Thread waiter = new Thread(() -> {
            try {
                System.out.println("waiter trying lockInterruptibly");
                LOCK.lockInterruptibly();
                try {
                    System.out.println("waiter acquired");
                } finally {
                    System.out.println("waiter unlock");
                    LOCK.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("waiter interrupted");
            }
        }, "waiter");

        holder.start();
        Thread.sleep(100);
        waiter.start();
        Thread.sleep(200);
        waiter.interrupt();

        holder.join();
        waiter.join();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
