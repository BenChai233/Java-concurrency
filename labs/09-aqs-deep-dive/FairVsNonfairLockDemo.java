// legacy version (kept for reference)
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 公平锁与非公平锁对比演示
 *
 * 该程序演示了公平锁和非公平锁在获取锁时的行为差异。
 * - 公平锁：线程按照请求锁的顺序获取锁（FIFO队列）
 * - 非公平锁：线程可能插队获取锁，不严格按请求顺序
 *
 * 实验设计：
 * - waiters：排队等待锁的线程
 * - bargers：插队线程，在锁被释放时尝试获取锁
 * - 通过对比两种锁的行为，观察插队线程是否能成功获取锁
 */
class FairVsNonfairLockDemo {
    public static void main(String[] args) throws Exception {
        // 解析命令行参数，设置实验参数
        int waiters = args.length > 0 ? Integer.parseInt(args[0]) : 6; // 等待线程数
        int bargers = args.length > 1 ? Integer.parseInt(args[1]) : 2; // 插队线程数
        int rounds = args.length > 2 ? Integer.parseInt(args[2]) : 10; // 实验轮数
        int holdMs = args.length > 3 ? Integer.parseInt(args[3]) : 20; // 持有锁的时间（毫秒）

        // 分别测试公平锁和非公平锁的行为
        runRounds("fair", new ReentrantLock(true), waiters, bargers, rounds, holdMs);   // 公平锁
        runRounds("nonfair", new ReentrantLock(false), waiters, bargers, rounds, holdMs); // 非公平锁
    }
    /**
     * 执行多轮实验并统计结果
     *
     * @param label 锁类型标签（"fair" 或 "nonfair"）
     * @param lock 要测试的锁实例
     * @param waiters 等待线程数量
     * @param bargers 插队线程数量
     * @param rounds 实验轮数
     * @param holdMs 线程持有锁的时间（毫秒）
     * @throws Exception 异常
     */
    private static void runRounds(
            String label,
            ReentrantLock lock,
            int waiters,
            int bargers,
            int rounds,
            int holdMs
    ) throws Exception {
        int bargedFirstCount = 0; // 统计插队线程成为第一个获取锁的次数
        int bargerInFirstWaitersCount = 0; // 统计插队线程出现在前waiters个获取锁的次数

        System.out.println("== " + label + " ==");
        System.out.println("waiters=" + waiters + " bargers=" + bargers + " rounds=" + rounds + " holdMs=" + holdMs);

        for (int r = 1; r <= rounds; r++) {
            Result result = runOneRound(lock, waiters, bargers, holdMs);
            if (result.bargedFirst) {
                bargedFirstCount++; // 如果插队线程第一个获取锁，计数器加1
            }
            if (result.bargerInFirstWaiters) {
                bargerInFirstWaitersCount++; // 如果插队线程出现在前waiters个获取锁的线程中，计数器加1
            }

            // 只在第一轮或有插队线程出现时打印详细信息
            boolean printThisRound = r == 1 || result.bargerInFirstWaiters;
            if (printThisRound) {
                System.out.println("round=" + r
                        + " bargedFirst=" + result.bargedFirst
                        + " bargerInFirstWaiters=" + result.bargerInFirstWaiters);
                System.out.println("arrive=" + format(result.arriveOrder)); // 打印线程到达顺序
                System.out.println("acquire=" + format(result.acquireOrder)); // 打印线程获取锁顺序
            }
        }

        // 打印实验结果统计
        System.out.println("summary bargedFirst=" + bargedFirstCount + "/" + rounds
                + " bargerInFirstWaiters=" + bargerInFirstWaitersCount + "/" + rounds);
        System.out.println();
    }

    /**
     * 执行单轮实验
     *
     * @param lock 要测试的锁实例
     * @param waiters 等待线程数量
     * @param bargers 插队线程数量
     * @param holdMs 线程持有锁的时间（毫秒）
     * @return 实验结果
     * @throws Exception 异常
     */
    private static Result runOneRound(ReentrantLock lock, int waiters, int bargers, int holdMs) throws Exception {
        int total = waiters + bargers; // 总线程数

        String[] arriveOrder = new String[total]; // 记录线程到达顺序的数组
        String[] acquireOrder = new String[total]; // 记录线程获取锁顺序的数组
        AtomicInteger arriveSeq = new AtomicInteger(0); // 到达顺序序号计数器
        AtomicInteger acquireSeq = new AtomicInteger(0); // 获取锁顺序序号计数器

        CountDownLatch waitersStart = new CountDownLatch(1); // 控制等待线程开始的门闩
        CountDownLatch allDone = new CountDownLatch(total); // 等待所有线程完成的门闩
        CountDownLatch unlockSignal = new CountDownLatch(1); // 控制插队线程开始的门闩

        lock.lock(); // 获取锁，让等待线程进入等待状态
        try {
            // 创建并启动等待线程（W0, W1, ..., Wn-1）
            for (int i = 0; i < waiters; i++) {
                String name = "W" + i;
                Thread t = new Thread(() -> {
                    try {
                        waitersStart.await(); // 等待开始信号
                        record(arriveOrder, arriveSeq, name); // 记录到达顺序

                        lock.lock(); // 尝试获取锁
                        try {
                            record(acquireOrder, acquireSeq, name); // 记录获取锁顺序
                            Thread.sleep(holdMs); // 模拟持有锁的时间
                        } finally {
                            lock.unlock(); // 释放锁
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        allDone.countDown(); // 完成计数减1
                    }
                }, name);
                t.start();
            }

            waitersStart.countDown(); // 发送等待线程开始信号
            Thread.sleep(100); // 确保等待线程都进入等待状态

            // 创建并启动插队线程（B0, B1, ..., Bn-1）
            for (int i = 0; i < bargers; i++) {
                String name = "B" + i;
                Thread t = new Thread(() -> {
                    try {
                        unlockSignal.await(); // 等待解锁信号
                        while (lock.isLocked()) { // 自旋等待锁被释放
                            Thread.onSpinWait(); // 优化自旋等待
                        }

                        record(arriveOrder, arriveSeq, name); // 记录到达顺序
                        lock.lock(); // 尝试获取锁
                        try {
                            record(acquireOrder, acquireSeq, name); // 记录获取锁顺序
                            Thread.sleep(holdMs); // 模拟持有锁的时间
                        } finally {
                            lock.unlock(); // 释放锁
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        allDone.countDown(); // 完成计数减1
                    }
                }, name);
                t.start();
            }

            unlockSignal.countDown(); // 发送插队线程开始信号
        } finally {
            lock.unlock(); // 释放初始锁，让等待线程有机会获取锁
        }

        allDone.await(); // 等待所有线程完成

        // 检查是否插队线程第一个获取锁
        boolean bargedFirst = acquireOrder.length > 0 && acquireOrder[0] != null && acquireOrder[0].startsWith("B");
        boolean bargerInFirstWaiters = false; // 检查插队线程是否出现在前waiters个获取锁的线程中
        for (int i = 0; i < Math.min(waiters, acquireOrder.length); i++) {
            String who = acquireOrder[i];
            if (who != null && who.startsWith("B")) { // 如果在前waiters个位置中发现插队线程
                bargerInFirstWaiters = true;
                break;
            }
        }

        return new Result(arriveOrder, acquireOrder, bargedFirst, bargerInFirstWaiters);
    }

    /**
     * 记录线程事件到指定数组
     *
     * @param order 存储顺序的数组
     * @param seq 序号计数器（使用AtomicInteger确保线程安全）
     * @param value 要记录的值（线程名称）
     */
    private static void record(String[] order, AtomicInteger seq, String value) {
        int idx = seq.getAndIncrement(); // 获取并递增序号
        if (idx >= 0 && idx < order.length) { // 检查数组边界
            order[idx] = value; // 记录线程名称到对应位置
        }
    }

    /**
     * 格式化数组为字符串表示
     *
     * @param arr 要格式化的数组
     * @return 格式化后的字符串，形如 [item1, item2, ...]
     */
    private static String format(String[] arr) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", "); // 除第一个元素外，前面都加逗号和空格
            sb.append(arr[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * 实验结果记录类
     *
     * @param arriveOrder 线程到达顺序数组
     * @param acquireOrder 线程获取锁顺序数组
     * @param bargedFirst 插队线程是否第一个获取锁
     * @param bargerInFirstWaiters 插队线程是否出现在前waiters个获取锁的线程中
     */
    private record Result(String[] arriveOrder, String[] acquireOrder, boolean bargedFirst, boolean bargerInFirstWaiters) {}
}
