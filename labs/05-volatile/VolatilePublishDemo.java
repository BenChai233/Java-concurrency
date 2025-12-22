public class VolatilePublishDemo {
    private static final class Holder {
        int a;
        int b;

        Holder(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    private static volatile Holder volatileRef;
    private static Holder plainRef;

    public static void main(String[] args) throws Exception {
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;

        int plainErrors = runTest(iterations, false);
        int volatileErrors = runTest(iterations, true);

        System.out.println("plain errors=" + plainErrors);
        System.out.println("volatile errors=" + volatileErrors);
    }

    private static int runTest(int iterations, boolean useVolatile) throws Exception {
        int errors = 0;
        for (int i = 0; i < iterations; i++) {
            if (useVolatile) {
                volatileRef = null;
            } else {
                plainRef = null;
            }

            final boolean[] error = new boolean[1];

            Thread writer = new Thread(() -> {
                Holder h = new Holder(1, 2);
                if (useVolatile) {
                    volatileRef = h;
                } else {
                    plainRef = h;
                }
            }, "writer");

            Thread reader = new Thread(() -> {
                Holder h;
                if (useVolatile) {
                    while ((h = volatileRef) == null) {
                        Thread.onSpinWait();
                    }
                } else {
                    while ((h = plainRef) == null) {
                        Thread.onSpinWait();
                    }
                }
                if (h.a != 1 || h.b != 2) {
                    error[0] = true;
                }
            }, "reader");

            writer.start();
            reader.start();
            writer.join();
            reader.join();

            if (error[0]) {
                errors++;
            }
        }
        return errors;
    }
}
