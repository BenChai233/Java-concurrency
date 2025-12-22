public class ThreadLocalWithInitialDemo {
    public static void main(String[] args) throws Exception {
        ThreadLocal<Integer> local = ThreadLocal.withInitial(() -> 42);

        System.out.println("main initial=" + local.get());
        local.set(7);
        System.out.println("main after set=" + local.get());

        Thread worker = new Thread(() -> {
            System.out.println("worker initial=" + local.get());
        }, "worker");
        worker.start();
        worker.join();
    }
}
