import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ConcurrentHashMapWeaklyConsistentIterationDemo {
    public static void main(String[] args) throws Exception {
        int initial = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
        int adds = args.length > 1 ? Integer.parseInt(args[1]) : 500;

        ConcurrentHashMap<Integer, Integer> map = new ConcurrentHashMap<>();
        for (int i = 0; i < initial; i++) {
            map.put(i, i);
        }

        CountDownLatch start = new CountDownLatch(1);
        Thread writer = new Thread(() -> {
            try {
                start.await();
                for (int i = initial; i < initial + adds; i++) {
                    map.put(i, i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "writer");
        writer.start();

        start.countDown();
        int iterated = 0;
        for (var entry : map.entrySet()) {
            iterated++;
        }
        writer.join();

        System.out.println("initial=" + initial +
                " adds=" + adds +
                " iteratedCount=" + iterated +
                " finalSize=" + map.size());
    }
}
