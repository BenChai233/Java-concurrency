import java.time.Duration;

public class InterruptScenariosDemo {
    public static void main(String[] args) throws Exception {
        System.out.println("== InterruptScenariosDemo (JDK " + System.getProperty("java.version") + ") ==");
        System.out.println("Goal: compare interrupting sleep vs interrupting a busy loop; observe flag behavior.");
        System.out.println();

        interruptSleepingThread();
        System.out.println();
        interruptBusyLoopThread();
        System.out.println();
        interruptedClearsFlag();
        System.out.println();
        System.out.println("All demos finished.");
    }

    private static void interruptSleepingThread() throws Exception {
        System.out.println("-- demo: interrupt sleep -> InterruptedException + flag cleared --");
        Thread sleepy = new Thread(() -> {
            System.out.println(ts() + " [sleepy] start; isInterrupted=" + Thread.currentThread().isInterrupted());
            try {
                Thread.sleep(Duration.ofSeconds(5).toMillis());
                System.out.println(ts() + " [sleepy] woke normally (unexpected for this demo)");
            } catch (InterruptedException e) {
                System.out.println(ts() + " [sleepy] caught InterruptedException; isInterrupted=" + Thread.currentThread().isInterrupted());
                Thread.currentThread().interrupt();
                System.out.println(ts() + " [sleepy] re-interrupt; isInterrupted=" + Thread.currentThread().isInterrupted());
            }
            System.out.println(ts() + " [sleepy] exit");
        }, "sleepy");

        sleepy.start();
        Thread.sleep(200);
        System.out.println(ts() + " [main] interrupt sleepy");
        sleepy.interrupt();
        sleepy.join();
    }

    private static void interruptBusyLoopThread() throws Exception {
        System.out.println("-- demo: interrupt busy loop -> you must check flag and exit --");
        Thread busy = new Thread(() -> {
            long iterations = 0;
            long checksum = 0;
            long startNanos = System.nanoTime();
            while (!Thread.currentThread().isInterrupted()) {
                iterations++;
                checksum += (iterations & 7);
            }
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            System.out.println(ts() + " [busy] observed interrupt; isInterrupted=" + Thread.currentThread().isInterrupted());
            System.out.println(ts() + " [busy] iterations=" + iterations + ", checksum=" + checksum + ", elapsedMs=" + elapsedMs);
        }, "busy");

        busy.start();
        Thread.sleep(200);
        System.out.println(ts() + " [main] interrupt busy");
        busy.interrupt();
        busy.join();
    }

    private static void interruptedClearsFlag() throws Exception {
        System.out.println("-- demo: Thread.interrupted() reads + clears current thread flag --");
        Thread t = new Thread(() -> {
            Thread.currentThread().interrupt();
            System.out.println(ts() + " [t] after interrupt; isInterrupted=" + Thread.currentThread().isInterrupted());
            boolean wasInterrupted = Thread.interrupted();
            System.out.println(ts() + " [t] Thread.interrupted()=" + wasInterrupted);
            System.out.println(ts() + " [t] after Thread.interrupted(); isInterrupted=" + Thread.currentThread().isInterrupted());
        }, "clear-flag");
        t.start();
        t.join();
    }

    private static String ts() {
        return String.valueOf(System.currentTimeMillis());
    }
}
