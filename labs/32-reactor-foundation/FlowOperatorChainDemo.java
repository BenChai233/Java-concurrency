import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class FlowOperatorChainDemo {
    public static void main(String[] args) throws Exception {
        int total = args.length >= 1 ? Integer.parseInt(args[0]) : 20;
        int batchSize = args.length >= 2 ? Integer.parseInt(args[1]) : 4;
        int processingMs = args.length >= 3 ? Integer.parseInt(args[2]) : 50;

        SubmissionPublisher<Integer> source = new SubmissionPublisher<>();
        CountDownLatch done = new CountDownLatch(1);

        MapProcessor<Integer, Integer> map1 = new MapProcessor<>("map(*2)", i -> i * 2);
        MapProcessor<Integer, String> map2 = new MapProcessor<>("map(toString)", i -> "v=" + i);

        map2.subscribe(new BatchingSlowSubscriber<>(batchSize, processingMs, done));
        map1.subscribe(map2);
        source.subscribe(map1);

        for (int i = 1; i <= total; i++) {
            source.submit(i);
        }
        source.close();

        done.await(10, TimeUnit.SECONDS);
    }

    private static final class MapProcessor<T, R> implements Flow.Processor<T, R> {
        private final String name;
        private final Function<T, R> mapper;
        private volatile Flow.Subscription upstream;
        private volatile Flow.Subscriber<? super R> downstream;

        private MapProcessor(String name, Function<T, R> mapper) {
            this.name = Objects.requireNonNull(name, "name");
            this.mapper = Objects.requireNonNull(mapper, "mapper");
        }

        @Override
        public void subscribe(Flow.Subscriber<? super R> subscriber) {
            this.downstream = Objects.requireNonNull(subscriber, "subscriber");
            Flow.Subscription upstreamSnapshot = upstream;
            if (upstreamSnapshot != null) {
                downstream.onSubscribe(new BridgeSubscription(upstreamSnapshot, name));
            }
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.upstream = Objects.requireNonNull(subscription, "subscription");
            Flow.Subscriber<? super R> downstreamSnapshot = downstream;
            if (downstreamSnapshot != null) {
                downstreamSnapshot.onSubscribe(new BridgeSubscription(subscription, name));
            }
        }

        @Override
        public void onNext(T item) {
            Flow.Subscriber<? super R> downstreamSnapshot = downstream;
            if (downstreamSnapshot == null) {
                return;
            }
            R mapped = mapper.apply(item);
            System.out.println(name + " onNext in=" + item + " out=" + mapped + " thread=" + Thread.currentThread().getName());
            downstreamSnapshot.onNext(mapped);
        }

        @Override
        public void onError(Throwable throwable) {
            Flow.Subscriber<? super R> downstreamSnapshot = downstream;
            if (downstreamSnapshot != null) {
                downstreamSnapshot.onError(throwable);
            }
        }

        @Override
        public void onComplete() {
            Flow.Subscriber<? super R> downstreamSnapshot = downstream;
            if (downstreamSnapshot != null) {
                downstreamSnapshot.onComplete();
            }
        }
    }

    private static final class BridgeSubscription implements Flow.Subscription {
        private final Flow.Subscription upstream;
        private final String operatorName;

        private BridgeSubscription(Flow.Subscription upstream, String operatorName) {
            this.upstream = upstream;
            this.operatorName = operatorName;
        }

        @Override
        public void request(long n) {
            System.out.println(operatorName + " request(" + n + ") thread=" + Thread.currentThread().getName());
            upstream.request(n);
        }

        @Override
        public void cancel() {
            System.out.println(operatorName + " cancel() thread=" + Thread.currentThread().getName());
            upstream.cancel();
        }
    }

    private static final class BatchingSlowSubscriber<T> implements Flow.Subscriber<T> {
        private final int batchSize;
        private final int processingMs;
        private final CountDownLatch done;
        private final AtomicInteger received = new AtomicInteger();
        private volatile Flow.Subscription subscription;

        private BatchingSlowSubscriber(int batchSize, int processingMs, CountDownLatch done) {
            this.batchSize = batchSize;
            this.processingMs = processingMs;
            this.done = done;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(batchSize);
        }

        @Override
        public void onNext(T item) {
            int n = received.incrementAndGet();
            System.out.println("downstream onNext=" + item + " index=" + n + " thread=" + Thread.currentThread().getName());

            try {
                Thread.sleep(processingMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (n % batchSize == 0) {
                subscription.request(batchSize);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
            done.countDown();
        }

        @Override
        public void onComplete() {
            System.out.println("downstream complete received=" + received.get());
            done.countDown();
        }
    }
}

