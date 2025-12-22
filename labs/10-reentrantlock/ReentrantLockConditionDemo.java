import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockConditionDemo {
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Condition AWAIT_A = LOCK.newCondition();
    private static final Condition AWAIT_B = LOCK.newCondition();
    private static boolean readyA = false;
    private static boolean readyB = false;

    public static void main(String[] args) throws Exception {
        CountDownLatch done = new CountDownLatch(2);

        Thread a = new Thread(() -> {
            LOCK.lock();
            try {
                while (!readyA) {
                    AWAIT_A.await();
                }
                System.out.println("A resumed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                LOCK.unlock();
                done.countDown();
            }
        }, "A");

        Thread b = new Thread(() -> {
            LOCK.lock();
            try {
                while (!readyB) {
                    AWAIT_B.await();
                }
                System.out.println("B resumed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                LOCK.unlock();
                done.countDown();
            }
        }, "B");

        a.start();
        b.start();

        Thread.sleep(200);
        LOCK.lock();
        try {
            readyA = true;
            AWAIT_A.signal();
        } finally {
            LOCK.unlock();
        }

        Thread.sleep(200);
        LOCK.lock();
        try {
            readyB = true;
            AWAIT_B.signal();
        } finally {
            LOCK.unlock();
        }

        done.await();
    }
}
