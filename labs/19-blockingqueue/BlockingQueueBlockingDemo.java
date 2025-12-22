import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockingQueueBlockingDemo {
    public static void main(String[] args) throws Exception {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(1);

        System.out.println("Phase 1: take blocks until put");
        CountDownLatch phase1 = new CountDownLatch(1);
        Thread consumer1 = new Thread(() -> {
            long start = System.nanoTime();
            try {
                String item = queue.take();
                long waitedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                System.out.println("consumer took '" + item + "' after " + waitedMs + " ms");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                phase1.countDown();
            }
        }, "consumer-1");
        consumer1.start();

        Thread.sleep(300);
        Thread producer1 = new Thread(() -> {
            try {
                queue.put("item-1");
                System.out.println("producer put item-1");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer-1");
        producer1.start();
        phase1.await();

        System.out.println();
        System.out.println("Phase 2: put blocks when queue is full");
        queue.put("item-0");
        CountDownLatch phase2 = new CountDownLatch(2);
        Thread producer2 = new Thread(() -> {
            long start = System.nanoTime();
            try {
                queue.put("item-2");
                long waitedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                System.out.println("producer put item-2 after " + waitedMs + " ms");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                phase2.countDown();
            }
        }, "producer-2");
        producer2.start();

        Thread.sleep(300);
        Thread consumer2 = new Thread(() -> {
            try {
                String item = queue.take();
                System.out.println("consumer took '" + item + "'");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                phase2.countDown();
            }
        }, "consumer-2");
        consumer2.start();

        phase2.await();
        System.out.println("done");
    }
}
