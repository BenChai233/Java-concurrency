import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowOffloadSubscriberDemo {
    public static void main(String[] args) throws Exception {
        int total = args.length >= 1 ? Integer.parseInt(args[0]) : 20;
        int processingMs = args.length >= 2 ? Integer.parseInt(args[1]) : 80;
        int cancelAfter = args.length >= 3 ? Integer.parseInt(args[2]) : 12;

        ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("worker-1");
            t.setDaemon(true);
            return t;
        });

        SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>();
        CountDownLatch done = new CountDownLatch(1);

        publisher.subscribe(new OffloadingSubscriber(worker, processingMs, cancelAfter, done));

        for (int i = 1; i <= total; i++) {
            publisher.submit(i);
        }
        publisher.close();

        done.await(10, TimeUnit.SECONDS);
        worker.shutdownNow();
    }

    private static final class OffloadingSubscriber implements Flow.Subscriber<Integer> {
        private final ExecutorService worker;
        private final int processingMs;
        private final int cancelAfter;
        private final CountDownLatch done;
        private final AtomicInteger received = new AtomicInteger();
        private final AtomicInteger inFlight = new AtomicInteger();
        private final AtomicBoolean completed = new AtomicBoolean();
        private volatile Flow.Subscription subscription;

        private OffloadingSubscriber(ExecutorService worker, int processingMs, int cancelAfter, CountDownLatch done) {
            this.worker = worker;
            this.processingMs = processingMs;
            this.cancelAfter = cancelAfter;
            this.done = done;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(Integer item) {
            int n = received.incrementAndGet();
            String publisherThread = Thread.currentThread().getName();
            inFlight.incrementAndGet();

            worker.execute(() -> {
                try {
                    System.out.println("worker onNext=" + item + " index=" + n + " publisherThread=" + publisherThread + " workerThread=" + Thread.currentThread().getName());
                    Thread.sleep(processingMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    int left = inFlight.decrementAndGet();
                    if (completed.get() && left == 0) {
                        done.countDown();
                    }
                }

                if (n >= cancelAfter) {
                    System.out.println("worker cancel at index=" + n);
                    subscription.cancel();
                    completed.set(true);
                    if (inFlight.get() == 0) {
                        done.countDown();
                    }
                    return;
                }

                subscription.request(1);
            });
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
            completed.set(true);
            if (inFlight.get() == 0) {
                done.countDown();
            }
        }

        @Override
        public void onComplete() {
            System.out.println("publisher complete (downstream may still be draining)");
            completed.set(true);
            if (inFlight.get() == 0) {
                done.countDown();
            }
        }
    }
}

