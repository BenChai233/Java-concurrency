import java.util.concurrent.CountDownLatch;

public class VolatileReorderingDemo {
    private static final class Plain {
        int x;
        int y;
    }

    private static final class WithVolatile {
        volatile int x;
        volatile int y;
    }

    private static final class Result {
        int r1;
        int r2;
    }

    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;

        boolean plainObserved = runPlain(iterations);
        boolean volatileObserved = runVolatile(iterations);

        System.out.println("plain observed r1=0,r2=0: " + plainObserved);
        System.out.println("volatile observed r1=0,r2=0: " + volatileObserved);
    }

    private static boolean runPlain(int iterations) throws Exception {
        for (int i = 0; i < iterations; i++) {
            Plain shared = new Plain();
            Result result = new Result();
            if (runOnce(shared, result)) {
                System.out.println("plain hit at iteration=" + i);
                return true;
            }
        }
        return false;
    }

    private static boolean runVolatile(int iterations) throws Exception {
        for (int i = 0; i < iterations; i++) {
            WithVolatile shared = new WithVolatile();
            Result result = new Result();
            if (runOnce(shared, result)) {
                System.out.println("volatile hit at iteration=" + i);
                return true;
            }
        }
        return false;
    }

    private static boolean runOnce(Object shared, Result result) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                start.await();
                if (shared instanceof Plain) {
                    Plain p = (Plain) shared;
                    result.r1 = p.y;
                    p.x = 1;
                } else {
                    WithVolatile v = (WithVolatile) shared;
                    result.r1 = v.y;
                    v.x = 1;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            try {
                start.await();
                if (shared instanceof Plain) {
                    Plain p = (Plain) shared;
                    result.r2 = p.x;
                    p.y = 1;
                } else {
                    WithVolatile v = (WithVolatile) shared;
                    result.r2 = v.x;
                    v.y = 1;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, "t2");

        t1.start();
        t2.start();
        start.countDown();
        done.await();

        return result.r1 == 0 && result.r2 == 0;
    }
}
