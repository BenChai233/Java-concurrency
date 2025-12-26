public class VolatileStopFlagDemo {
    private static final class Worker implements Runnable {
        private boolean stop;
        private volatile boolean vstop;
        private volatile boolean safetyStop;
        private long iterations;
        private final boolean useVolatile;

        Worker(boolean useVolatile) {
            this.useVolatile = useVolatile;
        }

        @Override
        public void run() {
            // 将逻辑拆分为两个独立的循环，帮助 JIT 识别并优化
            if (useVolatile) {
                while (!vstop) {
                    iterations++;
                }
            } else {
                // 对于普通变量，JIT 极大概率会将 stop 变量提升到循环外（Hoisting），
                // 变成类似 if (!stop) while(true) { iterations++; } 的逻辑
                while (!stop) {
                    iterations++;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        runVolatile();
        System.out.println("-------------------");
        runPlain();
    }

    private static void runVolatile() throws Exception {
        Worker worker = new Worker(true);
        Thread t = new Thread(worker, "volatile-worker");
        long begin = System.nanoTime();
        t.start();
        Thread.sleep(200);
        worker.vstop = true;
        t.join(1000);
        long ms = (System.nanoTime() - begin) / 1_000_000;

        System.out.println("volatile stopped=" + !t.isAlive() +
                " iterations=" + worker.iterations + " ms=" + ms);
    }

    private static void runPlain() throws Exception {
        Worker worker = new Worker(false);
        Thread t = new Thread(worker, "plain-worker");
        long begin = System.nanoTime();
        t.start();
        
        // 增加预热时间，确保 JIT 编译器有足够的时间介入并优化循环
        Thread.sleep(1000); 
        
        System.out.println("主线程尝试停止 plain-worker...");
        worker.stop = true;
        
        // 等待 worker 响应
        t.join(2000);

        boolean stoppedQuickly = !t.isAlive();
        long ms = (System.nanoTime() - begin) / 1_000_000;
        System.out.println("plain stopped_quickly=" + stoppedQuickly +
                " iterations=" + worker.iterations + " ms=" + ms);
        
        if (!stoppedQuickly) {
            System.out.println("成功复现！线程陷入死循环，说明没看到 stop=true 的变化。");
            System.out.println("程序将在 1 秒后强制退出...");
            // 强制退出，否则进程挂着
            System.exit(0);
        } else {
            System.out.println("未能复现。可能原因：JIT 未触发优化或硬件缓存一致性生效太快。");
        }
    }
}
