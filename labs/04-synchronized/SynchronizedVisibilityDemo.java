public class SynchronizedVisibilityDemo {
    private static final Object LOCK = new Object();
    private static int value = 0;
    private static boolean ready = false;

    public static void main(String[] args) throws Exception {
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (LOCK) {
                value = 42;
                ready = true;
            }
        }, "writer");

        Thread reader = new Thread(() -> {
            long spins = 0;
            int observed = -1;
            while (true) {
                synchronized (LOCK) {
                    if (ready) {
                        observed = value;
                        break;
                    }
                }
                spins++;
            }
            System.out.println("observed=" + observed + " spins=" + spins);
        }, "reader");

        long begin = System.nanoTime();
        reader.start();
        writer.start();
        writer.join();
        reader.join();
        long ms = (System.nanoTime() - begin) / 1_000_000;

        System.out.println("done ms=" + ms);
    }
}
