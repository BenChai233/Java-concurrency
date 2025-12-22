import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class BlockingQueueThroughputCompareDemo {
    public static void main(String[] args) throws Exception {
        int totalOps = args.length > 0 ? Integer.parseInt(args[0]) : 200_000;
        int producers = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        int consumers = args.length > 2 ? Integer.parseInt(args[2]) : 2;
        int capacity = args.length > 3 ? Integer.parseInt(args[3]) : 1024;

        runOnce("ArrayBlockingQueue", new ArrayBlockingQueue<>(capacity), totalOps, producers, consumers, capacity);
        runOnce("LinkedBlockingQueue", new LinkedBlockingQueue<>(capacity), totalOps, producers, consumers, capacity);
    }

    private static void runOnce(
            String name,
            BlockingQueue<Integer> queue,
            int totalOps,
            int producers,
            int consumers,
            int capacity
    ) throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(producers + consumers);
        LongAdder consumed = new LongAdder();

        for (int i = 0; i < producers; i++) {
            int count = workCount(totalOps, producers, i);
            Thread producer = new Thread(() -> {
                await(startGate);
                try {
                    for (int j = 0; j < count; j++) {
                        queue.put(j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            }, name + "-producer-" + i);
            producer.start();
        }

        for (int i = 0; i < consumers; i++) {
            int count = workCount(totalOps, consumers, i);
            Thread consumer = new Thread(() -> {
                await(startGate);
                try {
                    for (int j = 0; j < count; j++) {
                        queue.take();
                    }
                    consumed.add(count);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            }, name + "-consumer-" + i);
            consumer.start();
        }

        long start = System.nanoTime();
        startGate.countDown();
        doneGate.await();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        double opsPerSec = totalOps / Math.max(1.0, elapsedMs / 1000.0);

        System.out.println(name + " result:");
        System.out.println("  totalOps=" + totalOps
                + " producers=" + producers
                + " consumers=" + consumers
                + " capacity=" + capacity
                + " consumed=" + consumed.sum()
                + " timeMs=" + elapsedMs
                + " opsPerSec=" + String.format("%.1f", opsPerSec));
    }

    private static int workCount(int total, int workers, int index) {
        int base = total / workers;
        int remainder = total % workers;
        return index == workers - 1 ? base + remainder : base;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
