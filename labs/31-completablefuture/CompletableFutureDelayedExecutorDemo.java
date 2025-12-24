import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class CompletableFutureDelayedExecutorDemo {
    public static void main(String[] args) {
        long start = System.nanoTime();
        Executor delayed = CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS);

        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> "hello", delayed);
        String result = cf.join();

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        System.out.println("result=" + result + " elapsedMs=" + elapsedMs);
    }
}
