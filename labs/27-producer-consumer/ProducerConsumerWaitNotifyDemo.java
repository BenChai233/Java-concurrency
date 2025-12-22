import java.util.concurrent.CountDownLatch;

public class ProducerConsumerWaitNotifyDemo {
    public static void main(String[] args) throws Exception {
        int capacity = args.length > 0 ? Integer.parseInt(args[0]) : 5;
        int producers = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        int consumers = args.length > 2 ? Integer.parseInt(args[2]) : 2;
        int itemsPerProducer = args.length > 3 ? Integer.parseInt(args[3]) : 10;

        int totalItems = producers * itemsPerProducer;
        int poisonPills = consumers;

        BoundedBuffer buffer = new BoundedBuffer(capacity);
        CountDownLatch producersDone = new CountDownLatch(producers);
        CountDownLatch consumersDone = new CountDownLatch(consumers);

        for (int i = 0; i < producers; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                for (int j = 0; j < itemsPerProducer; j++) {
                    buffer.put(id * 1000 + j);
                }
                System.out.println("producer-" + id + " done");
                producersDone.countDown();
            }, "producer-" + id);
            t.start();
        }

        for (int i = 0; i < consumers; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                int count = 0;
                while (true) {
                    int value = buffer.take();
                    if (value == -1) {
                        break;
                    }
                    count++;
                }
                System.out.println("consumer-" + id + " consumed=" + count);
                consumersDone.countDown();
            }, "consumer-" + id);
            t.start();
        }

        producersDone.await();
        for (int i = 0; i < poisonPills; i++) {
            buffer.put(-1);
        }
        consumersDone.await();

        System.out.println("totalItems=" + totalItems);
        System.out.println("done");
    }

    private static class BoundedBuffer {
        private final int[] items;
        private int head;
        private int tail;
        private int count;

        private BoundedBuffer(int capacity) {
            this.items = new int[capacity];
        }

        public synchronized void put(int item) {
            while (count == items.length) {
                waitUninterruptibly();
            }
            items[tail] = item;
            tail = (tail + 1) % items.length;
            count++;
            notifyAll();
        }

        public synchronized int take() {
            while (count == 0) {
                waitUninterruptibly();
            }
            int item = items[head];
            head = (head + 1) % items.length;
            count--;
            notifyAll();
            return item;
        }

        private void waitUninterruptibly() {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
