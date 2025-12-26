/**
 * VolatilePublishDemo - volatile变量发布测试类
 * 
 * 本类演示了volatile关键字在对象发布中的作用，特别是如何防止对象的不完全初始化被其他线程观察到。
 * 通过对比使用volatile和普通引用的场景，展示volatile如何保证可见性和防止指令重排序。
 * 
 * 在多线程环境中，当一个线程创建对象并将其引用发布给其他线程时，如果没有适当的同步机制，
 * 其他线程可能观察到未完全初始化的对象状态（例如，对象的字段值为默认值而不是构造函数中设置的值）。
 * volatile关键字通过保证可见性和禁止指令重排序来解决这个问题。
 */
public class VolatilePublishDemo {
    /**
     * Holder类：用于演示发布问题的简单容器类
     * 包含两个字段a和b，在构造函数中初始化
     */
    private static final class Holder {
        int a;
        int b;

        Holder(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    // 使用volatile修饰的引用，保证可见性和禁止指令重排序
    private static volatile Holder volatileRef;
    // 普通引用，不保证多线程环境下的可见性
    private static Holder plainRef;

    public static void main(String[] args) throws Exception {
        // 解析命令行参数作为迭代次数，如果没有提供则默认为10000
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;

        // 运行普通引用测试，统计错误次数
        int plainErrors = runTest(iterations, false);
        // 运行volatile引用测试，统计错误次数
        int volatileErrors = runTest(iterations, true);

        // 输出测试结果
        System.out.println("plain errors=" + plainErrors);
        System.out.println("volatile errors=" + volatileErrors);
    }

    /**
     * 运行并发测试的方法
     * @param iterations 测试迭代次数
     * @param useVolatile 是否使用volatile变量进行测试
     * @return 测试中发现的错误次数
     */
    private static int runTest(int iterations, boolean useVolatile) throws Exception {
        int errors = 0;
        for (int i = 0; i < iterations; i++) {
            // 每次迭代前重置引用为null，确保测试环境干净
            if (useVolatile) {
                volatileRef = null;
            } else {
                plainRef = null;
            }

            // 使用boolean数组作为线程间共享的错误标志，因为数组元素可以被修改
            final boolean[] error = new boolean[1];

            // 创建写线程：负责创建Holder对象并赋值给引用
            Thread writer = new Thread(() -> {
                Holder h = new Holder(1, 2); // 创建包含值1,2的Holder对象
                if (useVolatile) {
                    volatileRef = h; // 将对象引用赋值给volatile变量
                } else {
                    plainRef = h; // 将对象引用赋值给普通变量
                }
            }, "writer");

            // 创建读线程：负责读取引用并验证对象状态
            Thread reader = new Thread(() -> {
                Holder h;
                if (useVolatile) {
                    // 等待直到volatileRef不为null，使用onSpinWait优化自旋
                    while ((h = volatileRef) == null) {
                        // Thread.onSpinWait()是一个提示方法，告诉JVM当前线程正在自旋等待
                        // 允许JVM进行优化，例如在超线程处理器上让出执行单元给其他线程
                        Thread.onSpinWait();
                    }
                } else {
                    // 等待直到plainRef不为null，使用onSpinWait优化自旋
                    while ((h = plainRef) == null) {
                        // Thread.onSpinWait()是一个提示方法，告诉JVM当前线程正在自旋等待
                        // 允许JVM进行优化，例如在超线程处理器上让出执行单元给其他线程
                        Thread.onSpinWait();
                    }
                }
                // 验证读取到的对象状态是否正确，如果不正确则设置错误标志
                if (h.a != 1 || h.b != 2) {
                    error[0] = true;
                }
            }, "reader");

            // 启动写线程和读线程
            writer.start();
            reader.start();
            // 等待两个线程执行完成
            writer.join();
            reader.join();

            // 如果本次迭代中检测到错误，则错误计数加1
            if (error[0]) {
                errors++;
            }
        }
        return errors;
    }
}