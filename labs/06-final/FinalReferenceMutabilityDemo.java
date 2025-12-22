import java.util.ArrayList;
import java.util.List;

public class FinalReferenceMutabilityDemo {
    private final int[] numbers = new int[] {1, 2, 3};
    private final List<String> names = new ArrayList<>();

    public FinalReferenceMutabilityDemo() {
        names.add("A");
        names.add("B");
    }

    public static void main(String[] args) {
        FinalReferenceMutabilityDemo demo = new FinalReferenceMutabilityDemo();

        System.out.println("before numbers[0]=" + demo.numbers[0] + " names=" + demo.names);
        demo.numbers[0] = 99;
        demo.names.add("C");
        System.out.println("after  numbers[0]=" + demo.numbers[0] + " names=" + demo.names);
    }
}
