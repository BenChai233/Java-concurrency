public class FinalThisEscapeDemo {
    private static volatile Escapee shared;

    private static final class Escapee {
        final int finalValue;
        int plainValue;

        Escapee() {
            shared = this; // escape before initialization completes
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            plainValue = 1;
            finalValue = 1;
        }
    }

    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
        int plainBad = 0;
        int finalBad = 0;

        for (int i = 0; i < iterations; i++) {
            shared = null;

            Thread writer = new Thread(Escapee::new, "writer");
            Thread reader = new Thread(() -> {
                while (shared == null) {
                    Thread.onSpinWait();
                }
            }, "reader");

            writer.start();
            reader.start();
            writer.join();
            reader.join();

            Escapee e = shared;
            if (e.plainValue != 1) {
                plainBad++;
            }
            if (e.finalValue != 1) {
                finalBad++;
            }
        }

        System.out.println("plain bad=" + plainBad + " of " + iterations);
        System.out.println("final bad=" + finalBad + " of " + iterations);
        System.out.println("note: results are probabilistic; rerun if both are zero");
    }
}
