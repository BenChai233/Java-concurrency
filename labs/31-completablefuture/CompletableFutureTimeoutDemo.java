import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CompletableFutureTimeoutDemo {
    public static void main(String[] args) {
        CompletableFuture<String> slow = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "ok";
        });

        CompletableFuture<String> timeoutFail = slow.orTimeout(50, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> "timeout:" + ex.getClass().getSimpleName());

        CompletableFuture<String> timeoutDefault = slow.completeOnTimeout("default", 50, TimeUnit.MILLISECONDS);

        System.out.println("orTimeout -> " + timeoutFail.join());
        System.out.println("completeOnTimeout -> " + timeoutDefault.join());

        CompletableFuture<String> failed = CompletableFuture.failedFuture(new IllegalStateException("boom"));
        System.out.println("failedFuture -> " + failed.handle((v, ex) -> ex.getClass().getSimpleName()).join());
    }
}
