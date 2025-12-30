/**
 * Final 字段可见性演示
 * 
 * 该演示展示了 final 字段在 Java 内存模型中的特殊语义，
 * 以及与普通字段在多线程环境下的可见性差异。
 * 
 * 通过对比测试 final 字段和普通字段在并发访问下的行为，
 * 展示了 final 字段提供的安全性保证。
 */
public class FinalFieldVisibilityDemo {
    /**
     * 使用 final 字段的持有者类
     * final 字段在构造完成后对所有线程保证可见性
     */
    private static final class FinalHolder {
        final int finalValue;  // final 字段，保证初始化后对所有线程可见
        int plainValue;        // 普通字段

        FinalHolder() {
            this.finalValue = 1;  // 在构造函数中初始化 final 字段
            this.plainValue = 1;  // 在构造函数中初始化普通字段
        }
    }

    /**
     * 使用普通字段的持有者类（字段名保持为 finalValue 但无 final 修饰符）
     * 普通字段在多线程环境下可能不保证可见性
     */
    private static final class PlainHolder {
        int finalValue;  // 普通字段，不保证可见性
        int plainValue;  // 普通字段

        PlainHolder() {
            this.finalValue = 1;  // 在构造函数中初始化普通字段
            this.plainValue = 1;  // 在构造函数中初始化普通字段
        }
    }

    // 使用 final 字段的持有者实例
    private static FinalHolder finalHolder;
    // 使用普通字段的持有者实例
    private static PlainHolder plainHolder;

    /**
     * 主方法，运行 final 字段可见性测试
     * 
     * @param args 命令行参数，第一个参数为迭代次数
     * @throws Exception 可能抛出的异常
     */
    public static void main(String[] args) throws Exception {
        // 解析命令行参数，确定迭代次数
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;

        // 测试 final 字段的可见性
        int finalBad = testFinal(iterations);
        // 测试普通字段的可见性
        int plainBad = testPlain(iterations);

        // 输出测试结果
        System.out.println("final bad=" + finalBad + " of " + iterations);
        System.out.println("plain bad=" + plainBad + " of " + iterations);
        System.out.println("note: results are probabilistic; rerun if both are zero");
    }

    /**
     * 测试使用 final 字段的类的可见性
     * 
     * @param iterations 迭代次数
     * @return 出现错误的次数
     * @throws Exception 可能抛出的异常
     */
    private static int testFinal(int iterations) throws Exception {
        int bad = 0;  // 记录错误次数
        
        for (int i = 0; i < iterations; i++) {
            finalHolder = null;  // 重置共享引用

            // 写入线程：创建并发布 FinalHolder 实例
            Thread writer = new Thread(() -> finalHolder = new FinalHolder(), "writer");
            // 读取线程：等待对象被发布，然后读取字段值
            Thread reader = new Thread(() -> {
                // 自旋等待，直到 finalHolder 被赋值
                while (finalHolder == null) {
                    Thread.onSpinWait();  // 提示 CPU 进行自旋等待优化
                }
            }, "reader");

            // 启动线程
            writer.start();
            reader.start();
            // 等待线程完成
            writer.join();
            reader.join();

            // 检查字段值是否符合预期
            // finalValue 是 final 字段，应该保证对所有线程的可见性
            // plainValue 是普通字段，可能不会立即对其他线程可见
            FinalHolder h = finalHolder;
            if (h.finalValue != 1 || h.plainValue != 1) {
                bad++;  // 如果任一字段值不正确，计数器加1
            }
        }
        return bad;
    }

    /**
     * 测试使用普通字段的类的可见性（对比实验）
     * 
     * @param iterations 迭代次数
     * @return 出现错误的次数
     * @throws Exception 可能抛出的异常
     */
    private static int testPlain(int iterations) throws Exception {
        int bad = 0;  // 记录错误次数
        
        for (int i = 0; i < iterations; i++) {
            plainHolder = null;  // 重置共享引用

            // 写入线程：创建并发布 PlainHolder 实例
            Thread writer = new Thread(() -> plainHolder = new PlainHolder(), "writer");
            // 读取线程：等待对象被发布，然后读取字段值
            Thread reader = new Thread(() -> {
                // 自旋等待，直到 plainHolder 被赋值
                while (plainHolder == null) {
                    Thread.onSpinWait();  // 提示 CPU 进行自旋等待优化
                }
            }, "reader");

            // 启动线程
            writer.start();
            reader.start();
            // 等待线程完成
            writer.join();
            reader.join();

            // 检查字段值是否符合预期
            // finalValue 和 plainValue 都是普通字段，不保证对所有线程的可见性
            PlainHolder h = plainHolder;
            if (h.finalValue != 1 || h.plainValue != 1) {
                bad++;  // 如果任一字段值不正确，计数器加1
            }
        }
        return bad;
    }
}