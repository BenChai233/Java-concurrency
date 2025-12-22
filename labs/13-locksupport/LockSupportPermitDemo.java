import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class LockSupportPermitDemo {
    public static void main(String[] args) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch secondParkReady = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            ready.countDown();
            System.out.println("parking #1");
            long begin1 = System.nanoTime();
            LockSupport.park();
            long elapsed1 = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin1);
            System.out.println("unparked #1 after " + elapsed1 + "ms");

            secondParkReady.countDown();
            System.out.println("parking #2");
            long begin2 = System.nanoTime();
            LockSupport.park();
            long elapsed2 = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin2);
            System.out.println("unparked #2 after " + elapsed2 + "ms");
        }, "permit-worker");

        worker.start();
        ready.await();

        LockSupport.unpark(worker);
        LockSupport.unpark(worker);

        secondParkReady.await();
        TimeUnit.MILLISECONDS.sleep(200);
        LockSupport.unpark(worker);

        worker.join();
    }
}
