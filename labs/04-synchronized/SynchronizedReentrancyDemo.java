public class SynchronizedReentrancyDemo {
    private int depth;

    public static void main(String[] args) {
        int depth = args.length > 0 ? Integer.parseInt(args[0]) : 3;
        SynchronizedReentrancyDemo demo = new SynchronizedReentrancyDemo();
        demo.outer(depth);
        System.out.println("done depth=" + demo.depth);
    }

    private synchronized void outer(int remaining) {
        depth++;
        if (remaining > 0) {
            inner(remaining - 1);
        }
    }

    private synchronized void inner(int remaining) {
        depth++;
        if (remaining > 0) {
            outer(remaining - 1);
        }
    }
}
