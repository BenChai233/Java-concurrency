import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AQS共享锁与独占锁演示类
 * 通过Semaphore演示AQS的共享锁机制
 * 通过CountDownLatch演示AQS的独占锁机制
 */
public class AqsSharedExclusiveDemo {
    public static void main(String[] args) throws Exception {
        // 演示Semaphore（共享锁）的使用
        runSemaphoreDemo();
        // 演示CountDownLatch（独占锁）的使用
        runLatchDemo();
    }

    /**
     * 演示Semaphore（共享锁）的使用
     * Semaphore允许指定数量的线程同时访问共享资源
     * 在本例中，设置许可数为2，意味着最多2个线程可以同时访问资源
     */
    private static void runSemaphoreDemo() throws Exception {
        // 创建一个许可数为2的信号量，模拟最多2个线程可以同时访问资源
        Semaphore semaphore = new Semaphore(2);
        // 用于等待所有线程完成的计数器
        CountDownLatch done = new CountDownLatch(4);
        // 记录当前活跃的线程数
        AtomicInteger active = new AtomicInteger(0);
        // 记录最大并发线程数
        AtomicInteger maxActive = new AtomicInteger(0);

        // 创建4个线程，但只有2个许可，因此最多2个线程能同时执行
        for (int i = 0; i < 4; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                long acquireAt = 0L;
                try {
                    // 获取信号量许可，如果当前已有2个线程获得许可，则其他线程会阻塞等待
                    semaphore.acquire();
                    acquireAt = System.nanoTime();
                    // 增加活跃线程计数
                    int nowActive = active.incrementAndGet();
                    // 更新最大并发线程数
                    maxActive.getAndUpdate(prev -> Math.max(prev, nowActive));
                    System.out.println("semaphore acquired by t" + id +
                            " permits=" + semaphore.availablePermits() +
                            " active=" + nowActive);
                    // 模拟资源占用时间
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (acquireAt != 0L) {
                        // 减少活跃线程计数
                        active.decrementAndGet();
                    }
                    // 释放信号量许可，供其他等待的线程使用
                    semaphore.release();
                    // 表示当前线程已完成
                    done.countDown();
                }
            }, "s-" + id);
            t.start();
        }

        // 等待所有线程完成
        done.await();
        System.out.println("semaphore demo done, maxActive=" + maxActive.get());
    }

    /**
     * 演示CountDownLatch（独占锁）的使用
     * CountDownLatch是一个同步辅助类，允许一个或多个线程等待其他线程完成操作
     * 在本例中，使用CountDownLatch来协调多个线程的等待和释放
     */
    private static void runLatchDemo() throws Exception {
        // 创建一个计数为1的门闩，需要调用一次countDown()才能打开
        CountDownLatch latch = new CountDownLatch(1);
        // 用于等待所有线程完成的计数器
        CountDownLatch done = new CountDownLatch(4);

        // 创建3个等待线程，它们会等待门闩被打开
        for (int i = 0; i < 3; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                long startAt = System.nanoTime();
                try {
                    System.out.println("latch waiter t" + id + " waiting");
                    // 等待门闩计数变为0，会阻塞当前线程
                    latch.await();
                    long waitedMs = (System.nanoTime() - startAt) / 1_000_000;
                    System.out.println("latch waiter t" + id + " released waitedMs=" + waitedMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 表示当前线程已完成
                    done.countDown();
                }
            }, "l-" + id);
            t.start();
        }

        // 等待200ms，让等待线程先执行
        Thread.sleep(200);
        // 减少门闩计数，使等待的线程可以继续执行
        latch.countDown();

        // 创建一个延迟等待的线程，它会在门闩已经被打开后等待
        Thread late = new Thread(() -> {
            long startAt = System.nanoTime();
            try {
                System.out.println("latch late waiter waiting");
                // 由于门闩已经被打开（计数已为0），此线程不会阻塞
                latch.await();
                long waitedMs = (System.nanoTime() - startAt) / 1_000_000;
                System.out.println("latch late waiter released waitedMs=" + waitedMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // 表示当前线程已完成
                done.countDown();
            }
        }, "l-late");
        late.start();

        // 再次调用countDown()，实际上没有影响，因为计数已经为0
        latch.countDown();
        // 等待所有线程完成
        done.await();
        System.out.println("latch demo done");
    }
}
