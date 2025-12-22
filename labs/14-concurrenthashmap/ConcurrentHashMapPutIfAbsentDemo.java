import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentHashMapPutIfAbsentDemo {
    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 8;

        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger winners = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    start.await();
                    String value = "v" + id;
                    if (map.putIfAbsent("k", value) == null) {
                        winners.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "t" + i);
            t.start();
        }

        start.countDown();
        done.await();

        System.out.println("threads=" + threads +
                " winnerCount=" + winners.get() +
                " mapSize=" + map.size() +
                " value=" + map.get("k"));
    }
}
