import java.util.concurrent.CompletableFuture;

public class CompletableFutureBasicsDemo {
    public static void main(String[] args) {
        CompletableFuture<Integer> base = CompletableFuture.supplyAsync(() -> 1);

        CompletableFuture<Integer> applied = base.thenApply(v -> v + 1);
        CompletableFuture<Integer> composed = base.thenCompose(v -> CompletableFuture.supplyAsync(() -> v + 2));
        CompletableFuture<Integer> combined = applied.thenCombine(composed, Integer::sum);

        CompletableFuture<Integer> withError = CompletableFuture.<Integer>supplyAsync(() -> {
            throw new RuntimeException("boom");
        }).exceptionally(ex -> -1);

        CompletableFuture<Void> all = CompletableFuture.allOf(applied, composed, combined, withError);
        CompletableFuture<Object> any = CompletableFuture.anyOf(applied, composed, combined, withError);

        System.out.println("applied=" + applied.join());
        System.out.println("composed=" + composed.join());
        System.out.println("combined=" + combined.join());
        System.out.println("withError=" + withError.join());
        System.out.println("allDone=" + all.isDone());
        System.out.println("any=" + any.join());
    }
}
