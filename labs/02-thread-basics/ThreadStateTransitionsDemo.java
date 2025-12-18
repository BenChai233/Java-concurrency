import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class ThreadStateTransitionsDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("== ThreadStateTransitionsDemo (JDK " + System.getProperty("java.version") + ") ==");
        System.out.println("Goal: observe NEW/RUNNABLE/BLOCKED/WAITING/TIMED_WAITING/TERMINATED");
        System.out.println();

        demoSleepTimedWaiting();
        System.out.println();
        demoJoinWaiting();
        System.out.println();
        demoSynchronizedBlocked();
        System.out.println();
        demoWaitWaiting();
        System.out.println();
        System.out.println("All demos finished.");
    }

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
                () -> sleeper.start(),
                Duration.ofMillis(1200),
                Duration.ofMillis(10)
        );

        assertSeen(observed, Thread.State.TIMED_WAITING);
        System.out.println("Observed: " + observed);
    }

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

    private static Set<Thread.State> observeStates(
            Thread target,
            CountDownLatch started,
            Runnable startAction,
            Duration maxDuration,
            Duration sampleInterval
    ) throws InterruptedException {
        Set<Thread.State> observed = EnumSet.noneOf(Thread.State.class);
        Instant deadline = Instant.now().plus(maxDuration);

        startAction.run();
        started.await();

        while (Instant.now().isBefore(deadline)) {
            observed.add(target.getState());
            if (!target.isAlive()) {
                observed.add(Thread.State.TERMINATED);
                break;
            }
            Thread.sleep(sampleInterval.toMillis());
        }
        return observed;
    }

    private static void assertSeen(Set<Thread.State> observed, Thread.State expected) {
        if (!observed.contains(expected)) {
            throw new IllegalStateException("Expected to observe " + expected + ", but saw: " + observed);
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void waiterReadyToWaitAwait(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
