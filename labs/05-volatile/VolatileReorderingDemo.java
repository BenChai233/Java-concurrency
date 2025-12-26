import java.util.concurrent.CountDownLatch;

/**
 * 演示指令重排序（Instruction Reordering）现象及 volatile 的禁止重排序效果。
 * <p>
 * 经典案例：
 * 线程1执行：x = 1; r1 = y;
 * 线程2执行：y = 1; r2 = x;
 * <p>
 * 初始状态：x = 0, y = 0
 * <p>
 * 可能的结果：
 * 1. r1 = 0, r2 = 1 (t1 先执行完或 t1 运行到一半 t2 插入)
 * 2. r1 = 1, r2 = 0 (t2 先执行完或 t2 运行到一半 t1 插入)
 * 3. r1 = 1, r2 = 1 (t1 写 x, t2 写 y, 然后各自读)
 * <p>
 * 异常结果（重排序导致）：
 * 4. r1 = 0, r2 = 0
 * 原因：编译器或 CPU 对指令进行了重排序。
 * 例如 t1 变成了先读 y (此时为0)，再写 x。
 * t2 变成了先读 x (此时为0)，再写 y。
 * <p>
 * volatile 关键字通过内存屏障（Memory Barrier）禁止了这种重排序。
 */
public class VolatileReorderingDemo {
    // 普通变量，可能发生重排序
    private static final class Plain {
        int x;
        int y;
    }

    // volatile 变量，禁止特定类型的重排序
    private static final class WithVolatile {
        volatile int x;
        volatile int y;
    }

    private static final class Result {
        int r1;
        int r2;
    }

    public static void main(String[] args) throws Exception {
        // 迭代次数，默认 10 万次，因为重排序现象需要大量尝试才能捕捉到
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;

        System.out.println("开始测试普通变量（可能出现重排序）...");
        boolean plainObserved = runPlain(iterations);

        System.out.println("开始测试 volatile 变量（不应出现重排序）...");
        boolean volatileObserved = runVolatile(iterations);

        System.out.println("普通变量是否观察到 r1=0,r2=0: " + plainObserved);
        System.out.println("volatile 变量是否观察到 r1=0,r2=0: " + volatileObserved);
    }

    private static boolean runPlain(int iterations) throws Exception {
        for (int i = 0; i < iterations; i++) {
            Plain shared = new Plain();
            Result result = new Result();
            // 如果出现了 r1=0, r2=0，说明发生了重排序
            if (runOnce(shared, result)) {
                System.out.println("普通变量在第 " + i + " 次迭代出现了指令重排序现象 (r1=0, r2=0)");
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
                System.out.println("volatile 变量在第 " + i + " 次迭代出现了异常现象 (r1=0, r2=0) - 这不应该发生");
                return true;
            }
        }
        return false;
    }

    /**
     * 运行一次实验。
     * 返回 true 表示观察到了 r1=0 && r2=0 的现象（即发生了重排序）。
     */
    private static boolean runOnce(Object shared, Result result) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try {
                start.await(); // 等待主线程发令
                if (shared instanceof Plain) {
                    Plain p = (Plain) shared;
                    p.x = 1;      // 写 x
                    result.r1 = p.y; // 读 y
                } else {
                    WithVolatile v = (WithVolatile) shared;
                    v.x = 1;      // 写 volatile x
                    result.r1 = v.y; // 读 volatile y
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            try {
                start.await(); // 等待主线程发令
                if (shared instanceof Plain) {
                    Plain p = (Plain) shared;
                    p.y = 1;      // 写 y
                    result.r2 = p.x; // 读 x
                } else {
                    WithVolatile v = (WithVolatile) shared;
                    v.y = 1;      // 写 volatile y
                    result.r2 = v.x; // 读 volatile x
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        }, "t2");

        t1.start();
        t2.start();
        start.countDown(); // 两个线程同时开始
        done.await();      // 等待两个线程结束

        // 只有当 r1 == 0 且 r2 == 0 时，才意味着发生了重排序
        // 也就是 t1 读到了 y=0 (说明 t2 的 y=1 还没执行或没被看到)，且 t2 读到了 x=0 (说明 t1 的 x=1 还没执行或没被看到)
        // 在顺序一致性模型下，这是不可能的，因为必然有一个先写。
        return result.r1 == 0 && result.r2 == 0;
    }
}
