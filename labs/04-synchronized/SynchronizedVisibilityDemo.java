public class SynchronizedVisibilityDemo {
    private static final Object LOCK = new Object(); // 定义一个锁对象，用于同步
    private static int value = 0; // 共享变量，用于存储值
    private static boolean ready = false; // 共享变量，用于标识value是否已准备好

    public static void main(String[] args) throws Exception {
        // 创建写线程，负责修改value和ready变量
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100); // 模拟写线程的准备工作，延迟100毫秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 恢复中断状态
            }
            // 使用synchronized块确保对共享变量的修改是原子的
            synchronized (LOCK) {
                value = 42; // 设置value为42
                ready = true; // 设置ready为true，表示value已准备好
            }
        }, "writer");

        // 创建读线程，负责读取value和ready变量
        Thread reader = new Thread(() -> {
            long spins = 0; // 记录自旋次数
            int observed = -1; // 记录读取到的value值
            while (true) {
                // 使用synchronized块确保对共享变量的读取是原子的
                synchronized (LOCK) {
                    if (ready) { // 检查ready变量是否为true
                        observed = value; // 读取value的值
                        break; // 退出循环
                    }
                }
                spins++; // 自旋次数加1
            }
            System.out.println("observed=" + observed + " spins=" + spins); // 输出读取到的值和自旋次数
        }, "reader");

        long begin = System.nanoTime(); // 记录开始时间
        reader.start(); // 启动读线程
        writer.start(); // 启动写线程
        writer.join(); // 等待写线程结束
        reader.join(); // 等待读线程结束
        long ms = (System.nanoTime() - begin) / 1_000_000; // 计算执行时间（毫秒）

        System.out.println("done ms=" + ms); // 输出执行时间
    }
}
