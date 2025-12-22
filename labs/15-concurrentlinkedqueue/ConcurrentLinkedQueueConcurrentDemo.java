import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentLinkedQueueConcurrentDemo {
    public static void main(String[] args) throws Exception {
        int producers = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int perProducer = args.length > 1 ? Integer.parseInt(args[1]) : 100_000;
        int consumers = Math.max(2, producers / 2);

        ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch doneProducers = new CountDownLatch(producers);
        CountDownLatch doneConsumers = new CountDownLatch(consumers);
        AtomicInteger consumed = new AtomicInteger(0);

        for (int i = 0; i < producers; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < perProducer; j++) {
                        queue.offer(j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneProducers.countDown();
                }
            }, "producer-" + i);
            t.start();
        }

        int expected = producers * perProducer;
        for (int i = 0; i < consumers; i++) {
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    while (consumed.get() < expected) {
                        Integer v = queue.poll();
                        if (v != null) {
                            consumed.incrementAndGet();
                        } else {
                            Thread.yield();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneConsumers.countDown();
                }
            }, "consumer-" + i);
            t.start();
        }

        long begin = System.nanoTime();
        start.countDown();
        doneProducers.await();
        doneConsumers.await();
        long ms = (System.nanoTime() - begin) / 1_000_000;

        System.out.println("producers=" + producers +
                " perProducer=" + perProducer +
                " expected=" + expected +
                " consumed=" + consumed.get() +
                " elapsedMs=" + ms);
    }
}
