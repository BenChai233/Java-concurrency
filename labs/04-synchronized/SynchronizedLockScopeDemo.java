import java.util.concurrent.CountDownLatch;

public class SynchronizedLockScopeDemo {
    private static final class Locker {
        synchronized void instanceWork(long sleepMs) {
            sleep(sleepMs);
        }

        static synchronized void classWork(long sleepMs) {
            sleep(sleepMs);
        }
    }

    public static void main(String[] args) throws Exception {
        long sleepMs = args.length > 0 ? Long.parseLong(args[0]) : 300;

        testSameInstance(sleepMs);
        testDifferentInstances(sleepMs);
        testClassVsInstance(sleepMs);
    }

    private static void testSameInstance(long sleepMs) throws Exception {
        Locker locker = new Locker();
        Runnable a = () -> locker.instanceWork(sleepMs);
        Runnable b = () -> locker.instanceWork(sleepMs);
        runTwo("same_instance", a, b);
    }

    private static void testDifferentInstances(long sleepMs) throws Exception {
        Locker aLocker = new Locker();
        Locker bLocker = new Locker();
        Runnable a = () -> aLocker.instanceWork(sleepMs);
        Runnable b = () -> bLocker.instanceWork(sleepMs);
        runTwo("different_instances", a, b);
    }

    private static void testClassVsInstance(long sleepMs) throws Exception {
        Locker locker = new Locker();
        Runnable a = () -> Locker.classWork(sleepMs);
        Runnable b = () -> locker.instanceWork(sleepMs);
        runTwo("class_vs_instance", a, b);
    }

    private static void runTwo(String label, Runnable a, Runnable b) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        Thread t1 = new Thread(wrap(label, "t1", start, done, a), label + "-t1");
        Thread t2 = new Thread(wrap(label, "t2", start, done, b), label + "-t2");

        t1.start();
        t2.start();

        long begin = System.nanoTime();
        start.countDown();
        done.await();
        long ms = (System.nanoTime() - begin) / 1_000_000;

        System.out.println(label + " total_ms=" + ms);
    }

    private static Runnable wrap(String label, String name, CountDownLatch start,
                                 CountDownLatch done, Runnable body) {
        return () -> {
            try {
                start.await();
                long s = System.currentTimeMillis();
                System.out.println(label + " " + name + " start=" + s);
                body.run();
                long e = System.currentTimeMillis();
                System.out.println(label + " " + name + " end=" + e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        };
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
