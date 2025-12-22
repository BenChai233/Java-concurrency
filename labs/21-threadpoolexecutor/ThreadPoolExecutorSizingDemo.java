import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolExecutorSizingDemo {
    public static void main(String[] args) throws Exception {
        RejectedExecutionHandler rejectedHandler = (task, executor) ->
                System.out.println("rejected " + task);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                2,
                1,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                new NamedThreadFactory("pool-demo"),
                rejectedHandler
        );

        CountDownLatch done = new CountDownLatch(3);
        for (int i = 1; i <= 4; i++) {
            executor.execute(new DemoTask(i, done));
            printStatus("after submit " + i, executor);
        }

        done.await();
        printStatus("after tasks", executor);

        Thread.sleep(1500);
        printStatus("after keepAlive", executor);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    private static void printStatus(String label, ThreadPoolExecutor executor) {
        System.out.println(label
                + " poolSize=" + executor.getPoolSize()
                + " active=" + executor.getActiveCount()
                + " queueSize=" + executor.getQueue().size()
                + " completed=" + executor.getCompletedTaskCount());
    }

    private static class DemoTask implements Runnable {
        private final int id;
        private final CountDownLatch done;

        private DemoTask(int id, CountDownLatch done) {
            this.id = id;
            this.done = done;
        }

        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            System.out.println("start task-" + id + " on " + name);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.println("finish task-" + id + " on " + name);
                done.countDown();
            }
        }

        @Override
        public String toString() {
            return "task-" + id;
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger index = new AtomicInteger();
        private final String prefix;

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, prefix + "-" + index.incrementAndGet());
        }
    }
}
