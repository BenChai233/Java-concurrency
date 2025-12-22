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
            while (true) {
                if (useVolatile) {
                    if (vstop) {
                        break;
                    }
                } else {
                    if (stop) {
                        break;
                    }
                }
                iterations++;
                if ((iterations & 0xFFFFF) == 0 && safetyStop) {
                    break;
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        runVolatile();
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
        Thread.sleep(200);
        worker.stop = true;
        t.join(200);

        boolean stoppedQuickly = !t.isAlive();
        if (!stoppedQuickly) {
            worker.safetyStop = true;
            t.join(1000);
        }

        long ms = (System.nanoTime() - begin) / 1_000_000;
        System.out.println("plain stopped_quickly=" + stoppedQuickly +
                " iterations=" + worker.iterations + " ms=" + ms);
    }
}
