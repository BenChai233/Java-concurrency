import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionWhileGuardDemo {
    public static void main(String[] args) throws Exception {
        runIfVersion();
        System.out.println("----");
        runWhileVersion();
    }

    private static void runIfVersion() throws Exception {
        CounterBox box = new CounterBox();
        CountDownLatch waiting = new CountDownLatch(2);
        CountDownLatch consumed = new CountDownLatch(2);

        Thread c1 = new Thread(() -> box.takeWithIf(waiting, consumed), "if-c1");
        Thread c2 = new Thread(() -> box.takeWithIf(waiting, consumed), "if-c2");
        c1.start();
        c2.start();

        waiting.await();
        box.put(1);
        consumed.await();

        System.out.println("if version finalCount=" + box.getCount());
    }

    private static void runWhileVersion() throws Exception {
        CounterBox box = new CounterBox();
        CountDownLatch waiting = new CountDownLatch(2);
        CountDownLatch firstConsumed = new CountDownLatch(1);
        CountDownLatch secondConsumed = new CountDownLatch(1);

        Thread c1 = new Thread(() -> box.takeWithWhile(waiting, firstConsumed), "while-c1");
        Thread c2 = new Thread(() -> box.takeWithWhile(waiting, secondConsumed), "while-c2");
        c1.start();
        c2.start();

        waiting.await();
        box.put(1);
        firstConsumed.await();
        box.put(1);
        secondConsumed.await();

        System.out.println("while version finalCount=" + box.getCount());
    }

    private static class CounterBox {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notEmpty = lock.newCondition();
        private int count = 0;

        int getCount() {
            lock.lock();
            try {
                return count;
            } finally {
                lock.unlock();
            }
        }

        void put(int items) {
            lock.lock();
            try {
                count += items;
                notEmpty.signalAll();
            } finally {
                lock.unlock();
            }
        }

        void takeWithIf(CountDownLatch waiting, CountDownLatch consumed) {
            lock.lock();
            try {
                if (count == 0) {
                    waiting.countDown();
                    notEmpty.await();
                }
                count--;
                if (consumed != null) {
                    consumed.countDown();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }

        void takeWithWhile(CountDownLatch waiting, CountDownLatch consumed) {
            lock.lock();
            try {
                while (count == 0) {
                    waiting.countDown();
                    notEmpty.await();
                }
                count--;
                if (consumed != null) {
                    consumed.countDown();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }
    }
}
