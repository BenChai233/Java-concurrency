import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierDemo {
    public static void main(String[] args) throws Exception {
        int parties = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        CountDownLatch done = new CountDownLatch(parties);
        CyclicBarrier barrier = new CyclicBarrier(parties, () ->
                System.out.println("barrier tripped, next phase"));

        for (int i = 0; i < parties; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                try {
                    System.out.println("worker-" + id + " phase-1");
                    barrier.await();
                    System.out.println("worker-" + id + " phase-2");
                    barrier.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (BrokenBarrierException e) {
                    System.out.println("worker-" + id + " barrier broken");
                } finally {
                    done.countDown();
                }
            }, "worker-" + id);
            t.start();
        }

        done.await();
        System.out.println("all workers finished");
    }
}
