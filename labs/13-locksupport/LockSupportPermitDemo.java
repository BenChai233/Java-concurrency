import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class LockSupportPermitDemo {
    public static void main(String[] args) throws Exception {
        // 创建两个CountDownLatch实例，用于线程间的同步
        CountDownLatch ready = new CountDownLatch(1);          // 等待worker线程准备就绪
        CountDownLatch secondParkReady = new CountDownLatch(1); // 等待worker线程完成第一次park

        Thread worker = new Thread(() -> {
            // 通知主线程当前线程已准备好
            ready.countDown();
            System.out.println("parking #1");  // 第一次park
            long begin1 = System.nanoTime();   // 记录开始时间
            LockSupport.park();                // 阻塞当前线程，等待许可
            long elapsed1 = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin1); // 计算阻塞时间
            System.out.println("unparked #1 after " + elapsed1 + "ms"); // 输出第一次阻塞的时间

            // 通知主线程当前线程已完成第一次park，准备进行第二次park
            secondParkReady.countDown();
            System.out.println("parking #2");  // 第二次park
            long begin2 = System.nanoTime();   // 记录第二次开始时间
            LockSupport.park();                // 再次阻塞当前线程
            long elapsed2 = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin2); // 计算第二次阻塞时间
            System.out.println("unparked #2 after " + elapsed2 + "ms"); // 输出第二次阻塞的时间
        }, "permit-worker");

        worker.start();      // 启动工作线程
        ready.await();       // 等待工作线程准备就绪

        // 发送两次unpark信号，由于LockSupport的许可特性，
        LockSupport.unpark(worker);
        // 第二次 unpark 发现许可已可用，等于“丢失”。
        // 第二次 park() 由于前面第二个许可并没有“累加”，所以会真正阻塞，直到主线程最后一次 unpark(worker) 才返回。
        LockSupport.unpark(worker);
        // 等待工作线程完成第一次park
        secondParkReady.await();
        TimeUnit.MILLISECONDS.sleep(200);
        // 发送第三个许可，用于唤醒第二次park
        LockSupport.unpark(worker);
        // 等待工作线程结束
        worker.join();
    }
}
