import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionInterruptDemo {
    public static void main(String[] args) throws Exception {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        CountDownLatch waiting = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            lock.lock();
            try {
                waiting.countDown();
                condition.await();
                System.out.println("unexpected: await returned normally");
            } catch (InterruptedException e) {
                System.out.println("caught InterruptedException");
                System.out.println("interruptedAfterCatch=" + Thread.currentThread().isInterrupted());
            } finally {
                lock.unlock();
            }
        }, "await-worker");

        worker.start();
        waiting.await();
        worker.interrupt();
        worker.join();
    }
}
