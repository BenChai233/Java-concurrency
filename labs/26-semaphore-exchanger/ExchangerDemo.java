import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;

public class ExchangerDemo {
    public static void main(String[] args) throws Exception {
        Exchanger<String> exchanger = new Exchanger<>();

        Thread t1 = new Thread(() -> exchange("A", "A->B", exchanger), "thread-A");
        Thread t2 = new Thread(() -> exchange("B", "B->A", exchanger), "thread-B");
        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    private static void exchange(String name, String data, Exchanger<String> exchanger) {
        try {
            System.out.println(name + " before: " + data);
            String other = exchanger.exchange(data, 2, TimeUnit.SECONDS);
            System.out.println(name + " after: " + other);
        } catch (Exception e) {
            System.out.println(name + " exchange failed: " + e.getClass().getSimpleName());
        }
    }
}
