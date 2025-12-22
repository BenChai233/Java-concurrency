public class FinalFieldVisibilityDemo {
    private static final class FinalHolder {
        final int finalValue;
        int plainValue;

        FinalHolder() {
            this.finalValue = 1;
            this.plainValue = 1;
        }
    }

    private static final class PlainHolder {
        int finalValue;
        int plainValue;

        PlainHolder() {
            this.finalValue = 1;
            this.plainValue = 1;
        }
    }

    private static volatile FinalHolder finalHolder;
    private static volatile PlainHolder plainHolder;

    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;

        int finalBad = testFinal(iterations);
        int plainBad = testPlain(iterations);

        System.out.println("final bad=" + finalBad + " of " + iterations);
        System.out.println("plain bad=" + plainBad + " of " + iterations);
        System.out.println("note: results are probabilistic; rerun if both are zero");
    }

    private static int testFinal(int iterations) throws Exception {
        int bad = 0;
        for (int i = 0; i < iterations; i++) {
            finalHolder = null;

            Thread writer = new Thread(() -> finalHolder = new FinalHolder(), "writer");
            Thread reader = new Thread(() -> {
                while (finalHolder == null) {
                    Thread.onSpinWait();
                }
            }, "reader");

            writer.start();
            reader.start();
            writer.join();
            reader.join();

            FinalHolder h = finalHolder;
            if (h.finalValue != 1 || h.plainValue != 1) {
                bad++;
            }
        }
        return bad;
    }

    private static int testPlain(int iterations) throws Exception {
        int bad = 0;
        for (int i = 0; i < iterations; i++) {
            plainHolder = null;

            Thread writer = new Thread(() -> plainHolder = new PlainHolder(), "writer");
            Thread reader = new Thread(() -> {
                while (plainHolder == null) {
                    Thread.onSpinWait();
                }
            }, "reader");

            writer.start();
            reader.start();
            writer.join();
            reader.join();

            PlainHolder h = plainHolder;
            if (h.finalValue != 1 || h.plainValue != 1) {
                bad++;
            }
        }
        return bad;
    }
}
