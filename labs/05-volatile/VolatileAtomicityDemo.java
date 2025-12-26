import java.util.concurrent.CountDownLatch;

/**
 * 第 05 章实验：Volatile 原子性测试
 * <p>
 * 目标：验证 volatile 关键字虽然保证了可见性，但无法保证复合操作（如 i++）的原子性。
 * 现象：多线程并发执行 counter++ 后，最终结果通常小于预期值（出现写丢失）。
 */
public class VolatileAtomicityDemo {
    // 使用 volatile 修饰，保证可见性，但无法保证原子性
    private static volatile int counter = 0;

    public static void main(String[] args) throws Exception {
        // 解析参数：线程数（默认4），每个线程循环次数（默认100万）
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int loops = args.length > 1 ? Integer.parseInt(args[1]) : 1_000_000;

        // 用于控制所有线程同时开始（模拟并发瞬时压力）
        CountDownLatch start = new CountDownLatch(1);
        // 用于等待所有线程结束
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                try {
                    // 等待主线程发令
                    start.await();
                    for (int j = 0; j < loops; j++) {
                        // 关键点：counter++ 是 "读-改-写" 三步复合操作
                        // 即使 counter 是 volatile 的，这三步之间也可能被其他线程打断，导致更新丢失
                        counter++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 任务完成，计数器减一
                    done.countDown();
                }
            }, "t-" + i);
            t.start();
        }

        long begin = System.nanoTime();
        // 发令，所有线程开始运行
        start.countDown();
        // 等待所有线程结束
        done.await();
        long end = System.nanoTime();

        int expected = threads * loops;
        long ms = (end - begin) / 1_000_000;
        
        // 输出结果：如果 actual < expected，证明 volatile 不能保证原子性
        System.out.println("threads=" + threads + " loops=" + loops +
                " expected=" + expected + " actual=" + counter + " ms=" + ms);
    }
}
