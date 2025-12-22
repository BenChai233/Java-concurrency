import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FutureTaskSingleExecutionDemo {
    public static void main(String[] args) throws Exception {
        AtomicInteger runCount = new AtomicInteger();
        Callable<Integer> work = () -> {
            int current = runCount.incrementAndGet();
            System.out.println("callable runs, count=" + current);
            TimeUnit.MILLISECONDS.sleep(300);
            return 42;
        };

        FutureTask<Integer> task = new FutureTask<>(work);
        CountDownLatch start = new CountDownLatch(1);
        Thread t1 = new Thread(() -> runTask(task, start), "runner-1");
        Thread t2 = new Thread(() -> runTask(task, start), "runner-2");
        t1.start();
        t2.start();
        start.countDown();

        int result = task.get();
        t1.join();
        t2.join();

        System.out.println("result=" + result);
        System.out.println("runCount=" + runCount.get());
        System.out.println("isDone=" + task.isDone() + " isCancelled=" + task.isCancelled());
    }

    private static void runTask(FutureTask<Integer> task, CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        task.run();
    }
}
