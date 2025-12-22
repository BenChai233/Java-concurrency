import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentLinkedQueueFifoDemo {
    public static void main(String[] args) {
        ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        queue.offer(4);
        queue.offer(5);

        while (!queue.isEmpty()) {
            System.out.println("poll=" + queue.poll());
        }
    }
}
