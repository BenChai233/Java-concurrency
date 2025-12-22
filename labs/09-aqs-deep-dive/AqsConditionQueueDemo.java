import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AqsConditionQueueDemo {
    public static void main(String[] args) throws Exception {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        try {
            condition.await();
        } catch (IllegalMonitorStateException e) {
            System.out.println("await without lock throws: " + e.getClass().getSimpleName());
        }

        Thread waiter = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("waiter awaiting");
                condition.await();
                System.out.println("waiter resumed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }, "waiter");

        waiter.start();
        Thread.sleep(200);

        lock.lock();
        try {
            System.out.println("signaling");
            condition.signal();
        } finally {
            lock.unlock();
        }

        waiter.join();
    }
}
