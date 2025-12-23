import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * 线程状态转换演示类
 * 演示Java线程的六种状态：NEW、RUNNABLE、BLOCKED、WAITING、TIMED_WAITING、TERMINATED
 */
public class ThreadStateTransitionsDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("== ThreadStateTransitionsDemo (JDK " + System.getProperty("java.version") + ") ==");
        System.out.println("Goal: observe NEW/RUNNABLE/BLOCKED/WAITING/TIMED_WAITING/TERMINATED");
        System.out.println();

        // 演示sleep方法导致的TIMED_WAITING状态
        demoSleepTimedWaiting();
        System.out.println();
        // 演示join方法导致的WAITING状态
        demoJoinWaiting();
        System.out.println();
        // 演示同步块竞争导致的BLOCKED状态
        demoSynchronizedBlocked();
        System.out.println();
        // 演示wait方法导致的WAITING状态
        demoWaitWaiting();
        System.out.println();
        System.out.println("All demos finished.");
    }

    /**
     * 演示sleep方法导致的TIMED_WAITING状态
     * 创建一个线程，在其中调用Thread.sleep()方法，观察其状态变为TIMED_WAITING
     */
    private static void demoSleepTimedWaiting() throws Exception {
        System.out.println("-- demo: sleep -> TIMED_WAITING --");
        CountDownLatch started = new CountDownLatch(1);
        Thread sleeper = new Thread(() -> {
            started.countDown();
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "sleeper");

        Set<Thread.State> observed = observeStates(
                sleeper,
                started,
                sleeper::start,
                Duration.ofMillis(1200),
                Duration.ofMillis(10)
        );

        assertSeen(observed, Thread.State.TIMED_WAITING);
        System.out.println("Observed: " + observed);
    }

    /**
     * 演示join方法导致的WAITING状态
     * 创建一个工作线程和一个等待线程，等待线程调用join()方法等待工作线程结束
     */
    private static void demoJoinWaiting() throws Exception {
        System.out.println("-- demo: join() -> WAITING --");
        CountDownLatch workerStarted = new CountDownLatch(1);
        Thread worker = new Thread(() -> {
            workerStarted.countDown();
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "worker");

        CountDownLatch joinerStarted = new CountDownLatch(1);
        Thread joiner = new Thread(() -> {
            joinerStarted.countDown();
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "joiner");

        worker.start();
        workerStarted.await();
        joiner.start();

        Set<Thread.State> observed = observeStates(
                joiner,
                joinerStarted,
                () -> {
                },
                Duration.ofMillis(1200),
                Duration.ofMillis(10)
        );

        assertSeen(observed, Thread.State.WAITING);
        worker.join();
        joiner.join();
        System.out.println("Observed: " + observed);
    }

    /**
     * 演示同步块竞争导致的BLOCKED状态
     * 创建两个线程，其中一个线程持有锁，另一个线程尝试获取锁时处于BLOCKED状态
     */
    private static void demoSynchronizedBlocked() throws Exception {
        System.out.println("-- demo: synchronized contention -> BLOCKED --");
        Object monitor = new Object();
        CountDownLatch holderEntered = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            synchronized (monitor) {
                holderEntered.countDown();
                sleepQuietly(700);
            }
        }, "holder");

        CountDownLatch contenderStarted = new CountDownLatch(1);
        Thread contender = new Thread(() -> {
            contenderStarted.countDown();
            synchronized (monitor) {
                // no-op
            }
        }, "contender");

        holder.start();
        holderEntered.await();
        contender.start();

        Set<Thread.State> observed = observeStates(
                contender,
                contenderStarted,
                () -> {
                },
                Duration.ofMillis(1200),
                Duration.ofMillis(5)
        );

        assertSeen(observed, Thread.State.BLOCKED);
        holder.join();
        contender.join();
        System.out.println("Observed: " + observed);
    }

    /**
     * 演示Object.wait()方法导致的WAITING状态
     * 创建一个等待线程和一个通知线程，等待线程调用wait()方法进入WAITING状态
     */
    private static void demoWaitWaiting() throws Exception {
        System.out.println("-- demo: Object.wait() -> WAITING --");
        Object monitor = new Object();
        CountDownLatch waiterReadyToWait = new CountDownLatch(1);

        Thread waiter = new Thread(() -> {
            synchronized (monitor) {
                waiterReadyToWait.countDown();
                try {
                    monitor.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "waiter");

        Thread notifier = new Thread(() -> {
            waiterReadyToWaitAwait(waiterReadyToWait);
            sleepQuietly(250);
            synchronized (monitor) {
                monitor.notify();
            }
        }, "notifier");

        waiter.start();
        notifier.start();

        Set<Thread.State> observed = observeStates(
                waiter,
                waiterReadyToWait,
                () -> {
                },
                Duration.ofMillis(1200),
                Duration.ofMillis(5)
        );

        assertSeen(observed, Thread.State.WAITING);
        waiter.join();
        notifier.join();
        System.out.println("Observed: " + observed);
    }

    /**
     * 观察线程状态的方法
     * 
     * @param target 要观察状态的目标线程
     * @param started 用于等待目标线程开始执行的门闩
     * @param startAction 启动目标线程的动作
     * @param maxDuration 观察的最大持续时间
     * @param sampleInterval 状态采样的时间间隔
     * @return 观察到的所有线程状态集合
     * @throws InterruptedException 线程被中断异常
     */
    private static Set<Thread.State> observeStates(
            Thread target,           // 要观察状态的目标线程
            CountDownLatch started,  // 用于等待目标线程开始执行的门闩
            Runnable startAction,    // 启动目标线程的动作
            Duration maxDuration,    // 观察的最大持续时间
            Duration sampleInterval  // 状态采样的时间间隔
    ) throws InterruptedException {
        Set<Thread.State> observed = EnumSet.noneOf(Thread.State.class); // 存储观察到的线程状态集合
        Instant deadline = Instant.now().plus(maxDuration);             // 计算观察截止时间

        startAction.run();  // 执行启动动作（例如：调用thread.start()）
        started.await();    // 等待目标线程真正开始执行，确保观察的时机准确

        while (Instant.now().isBefore(deadline)) {      // 在截止时间之前持续观察
            observed.add(target.getState());            // 获取并记录当前线程状态
            if (!target.isAlive()) {                    // 如果线程已终止
                observed.add(Thread.State.TERMINATED);  // 显式添加TERMINATED状态
                break;                                  // 退出观察循环
            }
            Thread.sleep(sampleInterval.toMillis());    // 按指定间隔休眠，避免过度占用CPU
        }
        return observed;    // 返回观察到的所有线程状态
    }

    /**
     * 验证是否观察到了期望的线程状态
     * 
     * @param observed 观察到的状态集合
     * @param expected 期望观察到的状态
     */
    private static void assertSeen(Set<Thread.State> observed, Thread.State expected) {
        if (!observed.contains(expected)) {
            throw new IllegalStateException("Expected to observe " + expected + ", but saw: " + observed);
        }
    }

    /**
     * 安静地睡眠指定时间，忽略中断异常
     * 
     * @param ms 睡眠时间（毫秒）
     */
    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 等待CountDownLatch计数器归零
     * 
     * @param latch 要等待的CountDownLatch
     */
    private static void waiterReadyToWaitAwait(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
