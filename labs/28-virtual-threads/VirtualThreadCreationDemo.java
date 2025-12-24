import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class VirtualThreadCreationDemo {
    private static long runPlatform(int threads) throws InterruptedException {
        List<Thread> list = new ArrayList<>(threads);
        long start = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            list.add(t);
            t.start();
        }
        for (Thread t : list) {
            t.join();
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private static long runVirtual(int threads) throws InterruptedException {
        List<Thread> list = new ArrayList<>(threads);
        long start = System.nanoTime();
        for (int i = 0; i < threads; i++) {
            Thread t = Thread.ofVirtual().unstarted(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            list.add(t);
            t.start();
        }
        for (Thread t : list) {
            t.join();
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 2000;

        long platformMs = runPlatform(threads);
        long virtualMs = runVirtual(threads);

        System.out.println("threads=" + threads);
        System.out.println("platform elapsedMs=" + platformMs);
        System.out.println("virtual elapsedMs=" + virtualMs);
    }
}
