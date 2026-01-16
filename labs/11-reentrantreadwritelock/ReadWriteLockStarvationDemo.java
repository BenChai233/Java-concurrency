import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 读写锁饥饿问题演示
 * 
 * 该实验用于验证在高并发读操作的情况下，写操作可能出现的饥饿问题。
 * 通过对比公平锁和非公平锁的表现，展示公平策略如何解决写线程饥饿问题。
 * 
 * 实验场景：多个读线程持续进行读操作，导致写线程难以获取锁
 */
public class ReadWriteLockStarvationDemo {
    public static void main(String[] args) throws Exception {
        // 解析命令行参数：读线程数、每次读操作持有锁的时间、写线程最大等待时间
        int readers = args.length > 0 ? Integer.parseInt(args[0]) : 6;
        int holdMs = args.length > 1 ? Integer.parseInt(args[1]) : 50;
        int maxWaitMs = args.length > 2 ? Integer.parseInt(args[2]) : 2000;

        // 分别测试非公平模式和公平模式下的表现
        run(false, readers, holdMs, maxWaitMs);  // 非公平锁
        run(true, readers, holdMs, maxWaitMs);   // 公平锁
    }

    /**
     * 执行一次读写锁饥饿实验
     *
     * @param fair      是否使用公平锁策略
     * @param readers   读线程的数量
     * @param holdMs    每次读操作持有锁的时间（毫秒）
     * @param maxWaitMs 写线程最大等待时间（毫秒）
     * @throws Exception 异常
     */
    private static void run(boolean fair, int readers, int holdMs, int maxWaitMs) throws Exception {
        // 创建公平或非公平的读写锁
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock(fair);
        Lock readLock = rwLock.readLock();
        Lock writeLock = rwLock.writeLock();

        // 同步辅助工具
        CountDownLatch start = new CountDownLatch(1);      // 控制所有读线程同时开始
        CountDownLatch firstRead = new CountDownLatch(1);  // 等待第一个读操作开始
        AtomicBoolean stop = new AtomicBoolean(false);     // 控制读线程停止标志

        // 创建并启动读线程
        Thread[] readerThreads = new Thread[readers];
        for (int i = 0; i < readers; i++) {
            Thread t = new Thread(() -> {
                try {
                    // 等待开始信号
                    start.await();
                    // 持续进行读操作直到收到停止信号
                    while (!stop.get()) {
                        readLock.lock();  // 获取读锁
                        try {
                            // 第一个读操作完成后发出信号
                            firstRead.countDown();
                            // 模拟读操作耗时
                            TimeUnit.MILLISECONDS.sleep(holdMs);
                        } finally {
                            readLock.unlock();  // 释放读锁
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "reader-" + i);
            readerThreads[i] = t;
            t.start();
        }

        // 发出开始信号，让所有读线程同时开始
        start.countDown();
        // 等待第一个读操作开始，确保读操作正在持续进行
        firstRead.await();

        // 测试写线程获取锁的等待时间
        long begin = System.nanoTime();
        // 尝试获取写锁，如果超过maxWaitMs则返回false
        boolean acquired = writeLock.tryLock(maxWaitMs, TimeUnit.MILLISECONDS);
        long waitMs = (System.nanoTime() - begin) / 1_000_000;
        if (acquired) {
            writeLock.unlock();  // 释放写锁
        }
        
        // 设置停止标志，让读线程退出循环
        stop.set(true);
        // 等待所有读线程结束
        for (Thread t : readerThreads) {
            t.join();
        }

        // 输出实验结果，包括是否公平、读线程数、读操作持续时间、
        // 写线程是否成功获取锁、写线程等待时间
        System.out.println("fair=" + fair + " readers=" + readers + " holdMs=" + holdMs
                + " writerAcquired=" + acquired + " writerWaitMs=" + waitMs);
    }
}
