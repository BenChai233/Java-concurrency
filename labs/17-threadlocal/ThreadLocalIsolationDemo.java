import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadLocalIsolationDemo {
    public static void main(String[] args) throws Exception {
        ThreadLocal<String> local = new ThreadLocal<>();
        ConcurrentHashMap<String, String> results = new ConcurrentHashMap<>();
        CountDownLatch done = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            local.set("A");
            results.put(Thread.currentThread().getName(), local.get());
            done.countDown();
        }, "worker-1");

        Thread t2 = new Thread(() -> {
            local.set("B");
            results.put(Thread.currentThread().getName(), local.get());
            done.countDown();
        }, "worker-2");

        t1.start();
        t2.start();
        done.await();

        System.out.println("main=" + local.get());
        System.out.println("worker-1=" + results.get("worker-1"));
        System.out.println("worker-2=" + results.get("worker-2"));
    }
}
