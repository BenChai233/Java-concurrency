import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

public class VarHandleAtomicDemo {
    static class Counter {
        volatile int value;
    }

    private static final VarHandle VALUE;

    static {
        try {
            VALUE = MethodHandles.lookup().findVarHandle(Counter.class, "value", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int iterations = args.length > 1 ? Integer.parseInt(args[1]) : 100_000;

        Counter counter = new Counter();
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread t = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    int prev;
                    do {
                        prev = (int) VALUE.getVolatile(counter);
                    } while (!VALUE.compareAndSet(counter, prev, prev + 1));
                }
            });
            list.add(t);
            t.start();
        }

        for (Thread t : list) {
            t.join();
        }

        int expected = threads * iterations;
        System.out.println("expected=" + expected + " actual=" + counter.value);
    }
}
