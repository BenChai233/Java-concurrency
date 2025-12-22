import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockReentrancyDemo {
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static int depth = 0;

    public static void main(String[] args) {
        recurse(3);
        System.out.println("done depth=" + depth);
    }

    private static void recurse(int remaining) {
        LOCK.lock();
        try {
            depth++;
            if (remaining > 0) {
                recurse(remaining - 1);
            }
        } finally {
            LOCK.unlock();
        }
    }
}
