import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FlowBackpressureDemo {
    public static void main(String[] args) throws Exception {
        SubmissionPublisher<Integer> publisher = new SubmissionPublisher<>();
        CountDownLatch done = new CountDownLatch(1);

        publisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;
            private int received;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(Integer item) {
                received++;
                System.out.println("onNext=" + item + " thread=" + Thread.currentThread().getName());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                done.countDown();
            }

            @Override
            public void onComplete() {
                System.out.println("complete received=" + received);
                done.countDown();
            }
        });

        for (int i = 0; i < 20; i++) {
            publisher.submit(i);
        }
        publisher.close();

        done.await(5, TimeUnit.SECONDS);
    }
}
