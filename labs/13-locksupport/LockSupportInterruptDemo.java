import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class LockSupportInterruptDemo {
    public static void main(String[] args) throws Exception {
        CountDownLatch parked = new CountDownLatch(1);
        CountDownLatch secondParked = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            System.out.println("parking #1");
            parked.countDown();
            LockSupport.park();
            System.out.println("after park #1 interrupted=" + Thread.currentThread().isInterrupted());

            System.out.println("clearing interrupt and parking #2");
            Thread.interrupted();
            secondParked.countDown();
            LockSupport.park();
            System.out.println("after park #2 interrupted=" + Thread.currentThread().isInterrupted());
        }, "interrupt-worker");

        worker.start();
        parked.await();
        TimeUnit.MILLISECONDS.sleep(100);
        worker.interrupt();

        secondParked.await();
        TimeUnit.MILLISECONDS.sleep(100);
        LockSupport.unpark(worker);

        worker.join();
    }
}
