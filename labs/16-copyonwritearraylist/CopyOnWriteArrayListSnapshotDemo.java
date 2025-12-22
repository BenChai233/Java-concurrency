import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CopyOnWriteArrayListSnapshotDemo {
    public static void main(String[] args) {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(List.of("A", "B", "C"));
        Iterator<String> iterator = list.iterator();

        list.add("D");

        int iterated = 0;
        while (iterator.hasNext()) {
            System.out.println("iter=" + iterator.next());
            iterated++;
        }
        System.out.println("iteratedCount=" + iterated + " listSizeNow=" + list.size());
    }
}
