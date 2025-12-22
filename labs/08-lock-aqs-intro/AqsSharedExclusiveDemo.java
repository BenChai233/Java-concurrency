import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class AqsSharedExclusiveDemo {
    public static void main(String[] args) throws Exception {
        runSemaphoreDemo();
        runLatchDemo();
    }

    private static void runSemaphoreDemo() throws Exception {
        Semaphore semaphore = new Semaphore(2);
        CountDownLatch done = new CountDownLatch(4);

        for (int i = 0; i < 4; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.println("semaphore acquired by t" + id +
                            " permits=" + semaphore.availablePermits());
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                    done.countDown();
                }
            }, "s-" + id);
            t.start();
        }

        done.await();
        System.out.println("semaphore demo done");
    }

    private static void runLatchDemo() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                try {
                    System.out.println("latch waiter t" + id + " waiting");
                    latch.await();
                    System.out.println("latch waiter t" + id + " released");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "l-" + id);
            t.start();
        }

        Thread.sleep(200);
        latch.countDown();
        done.await();
        System.out.println("latch demo done");
    }
}
