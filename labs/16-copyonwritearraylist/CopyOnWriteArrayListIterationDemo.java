import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class CopyOnWriteArrayListIterationDemo {
    public static void main(String[] args) throws Exception {
        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add(i);
        }

        Iterator<Integer> iterator = list.iterator();
        Thread modifier = new Thread(() -> {
            for (int i = 5; i < 10; i++) {
                list.add(i);
            }
        }, "modifier");
        modifier.start();

        int iterated = 0;
        while (iterator.hasNext()) {
            System.out.println("iter=" + iterator.next());
            iterated++;
        }
        modifier.join();

        System.out.println("iteratedCount=" + iterated + " listSizeNow=" + list.size());
    }
}
