/**
 * Final 字段 this 引用逸出演示
 * 
 * 该演示展示了当对象的 this 引用在构造过程中逸出时，
 * final 字段的安全保证会被破坏的情况。
 * 
 * 在正常情况下，final 字段在对象构造完成后对所有线程保证可见性，
 * 但如果在构造函数完成前将 this 引用暴露给其他线程，
 * 则其他线程可能观察到未初始化的字段值。
 */
public class FinalThisEscapeDemo {
    // volatile 引用确保对 shared 的修改对所有线程立即可见
    private static volatile Escapee shared;

    /**
     * 逸出对象类
     * 
     * 在构造函数中提前将 this 引用赋值给共享变量，
     * 演示 this 引用逸出对 final 字段安全保证的影响
     */
    private static final class Escapee {
        final int finalValue;  // final 字段，但在 this 逸出时可能被观察到默认值
        int plainValue;        // 普通字段

        Escapee() {
            // 在对象完全初始化之前将 this 引用逸出到共享变量
            // 这会破坏 final 字段的语义保证
            // 注意：在这个版本中，我们先设置字段值再进行 this 逸出
            // 但这仍然可能在某些 JVM 实现或条件下产生不可预测的结果
            finalValue = 1;      // 先设置 final 字段
            plainValue = 1;      // 再设置普通字段
            
            shared = this;       // 然后逸出 this 引用

            // 这里可以添加一些额外的操作
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 主方法，运行 final 字段 this 引用逸出测试
     * 
     * @param args 命令行参数，第一个参数为迭代次数
     * @throws Exception 可能抛出的异常
     */
    public static void main(String[] args) throws Exception {
        // 解析命令行参数，确定迭代次数
        int iterations = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
        
        // 计数器：记录观察到错误值的次数
        int plainBad = 0;  // 普通字段错误次数
        int finalBad = 0;  // final 字段错误次数

        // 进行多次迭代测试
        for (int i = 0; i < iterations; i++) {
            shared = null;  // 重置共享引用

            // 用于捕获读取线程观察到的值的容器
            final int[] observedPlain = {0};  // 观察到的普通字段值
            final int[] observedFinal = {0};  // 观察到的 final 字段值

            // 写入线程：创建 Escapee 实例（在此过程中 this 引用会逸出）
            Thread writer = new Thread(Escapee::new, "writer");
            
            // 读取线程：等待 shared 引用被赋值，然后立即读取字段值
            Thread reader = new Thread(() -> {
                // 忙等待直到 shared 变量变为非 null
                while (shared == null) {
                    Thread.onSpinWait();  // 提示 CPU 进行自旋等待优化
                }
                
                // 重要：在看到 shared != null 后立即读取字段
                // 如果构造尚未完成，这可能会观察到默认值（0）
                Escapee e = shared;
                observedPlain[0] = e.plainValue;  // 读取普通字段值
                observedFinal[0] = e.finalValue;  // 读取 final 字段值
            }, "reader");

            // 启动线程
            writer.start();
            reader.start();
            // 等待线程完成
            writer.join();
            reader.join();

            // 检查读取线程是否观察到了意外的（默认）值
            if (observedPlain[0] != 1) {
                plainBad++;  // 普通字段值错误，计数器加1
            }
            if (observedFinal[0] != 1) {
                finalBad++;  // final 字段值错误，计数器加1
            }
        }

        // 输出测试结果
        System.out.println("plain bad=" + plainBad + " of " + iterations);
        System.out.println("final bad=" + finalBad + " of " + iterations);
        System.out.println("note: results are probabilistic; rerun if both are zero");
    }
}