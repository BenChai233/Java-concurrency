import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class VarHandleAcquireReleaseDemo {
    static class Box {
        int data;
        int flag;
    }

    private static final VarHandle DATA;
    private static final VarHandle FLAG;

    static {
        try {
            DATA = MethodHandles.lookup().findVarHandle(Box.class, "data", int.class);
            FLAG = MethodHandles.lookup().findVarHandle(Box.class, "flag", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] args) throws Exception {
        int rounds = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;
        Box box = new Box();

        Thread writer = new Thread(() -> {
            for (int i = 0; i < rounds; i++) {
                DATA.setOpaque(box, i);
                FLAG.setRelease(box, 1);
                FLAG.setRelease(box, 0);
            }
        });

        Thread reader = new Thread(() -> {
            int observed = 0;
            for (int i = 0; i < rounds; i++) {
                while ((int) FLAG.getAcquire(box) == 0) {
                    Thread.onSpinWait();
                }
                int value = (int) DATA.getOpaque(box);
                observed = value;
                while ((int) FLAG.getAcquire(box) == 1) {
                    Thread.onSpinWait();
                }
            }
            System.out.println("lastObserved=" + observed);
        });

        writer.start();
        reader.start();
        writer.join();
        reader.join();
    }
}
