import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class SemaphoreDemo {
    public static void main(String[] args) throws Exception {
        int permits = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        int workers = args.length > 1 ? Integer.parseInt(args[1]) : 8;

        Semaphore semaphore = new Semaphore(permits);
        AtomicInteger inCritical = new AtomicInteger();
        AtomicInteger maxObserved = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(workers);

        for (int i = 0; i < workers; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                try {
                    semaphore.acquire();
                    int current = inCritical.incrementAndGet();
                    maxObserved.updateAndGet(prev -> Math.max(prev, current));
                    System.out.println("worker-" + id + " enter, inCritical=" + current);
                    Thread.sleep(200);
                    current = inCritical.decrementAndGet();
                    System.out.println("worker-" + id + " leave, inCritical=" + current);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                    done.countDown();
                }
            }, "worker-" + id);
            t.start();
        }

        done.await();
        System.out.println("permits=" + permits + " maxObserved=" + maxObserved.get());
    }
}
