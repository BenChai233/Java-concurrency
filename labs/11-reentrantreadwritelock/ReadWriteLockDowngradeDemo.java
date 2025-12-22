import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockDowngradeDemo {
    public static void main(String[] args) throws Exception {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        Lock readLock = lock.readLock();
        Lock writeLock = lock.writeLock();

        readLock.lock();
        try {
            boolean upgraded = writeLock.tryLock(200, TimeUnit.MILLISECONDS);
            System.out.println("upgrade from read to write success=" + upgraded);
            if (upgraded) {
                writeLock.unlock();
            }
        } finally {
            readLock.unlock();
        }

        writeLock.lock();
        try {
            System.out.println("write acquired");
            readLock.lock();
            try {
                System.out.println("downgraded to read");
            } finally {
                writeLock.unlock();
            }
            System.out.println("still holding read after write released");
        } finally {
            readLock.unlock();
        }
    }
}
