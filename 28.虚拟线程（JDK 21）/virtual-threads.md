# 28. 虚拟线程（JDK 21） #

> 目标：把「虚拟线程是什么、解决什么问题、怎么用、用的时候要注意什么」讲清楚，并能在本仓库 `labs/28-virtual-threads` 里跑出直观结果。

---

# 1. 先把问题讲明白：为什么我们需要虚拟线程？ #

在“传统并发”里，我们经常陷入一个两难：

- **线程很直观**：一个请求/一个任务对应一个线程，代码就是“从上到下写同步逻辑”。
- **线程也很昂贵**：平台线程（Platform Thread）基本上 = OS 线程；线程越多，内存、调度、上下文切换成本越高。

所以工程上常见的妥协是：

- 用 **线程池**（例如 `newFixedThreadPool`）控制并发量。
- 如果任务里有大量阻塞（sleep、等待 IO、等待锁、等待下游），线程池就会出现“**线程都在等**”的问题：线程数量被限制住了，但每个线程又大部分时间都在阻塞，吞吐上不去。

你会看到这些现象：

- 线程池排队：请求/任务堆在队列里等。
- 增大线程池：可能顶一阵，但很快遇到内存、调度、CPU 飙高等问题。
- 业务代码为了“少阻塞”而变复杂：回调、Reactive、状态机……（不是不好，但复杂度确实上来了）

**虚拟线程（Virtual Thread）的核心诉求**就是：

> 让“一个任务一条线程”的同步写法重新变得可扩展，尤其是对阻塞密集型（IO-bound / waiting-heavy）场景。

---

# 2. 虚拟线程是什么？先把几个名词对齐 #

在 JDK 21 里，Java 线程大体分两类：

- **平台线程（Platform Thread）**：和 OS 线程强绑定（1:1），这是你过去 `new Thread(...)` 创建出来的线程。
- **虚拟线程（Virtual Thread）**：由 JVM 管理与调度，不再要求和 OS 线程一一对应。

为了理解虚拟线程，要引入一个关键角色：

- **载体线程（Carrier Thread）**：真正跑在 OS 上的平台线程。虚拟线程会被 JVM “挂载（mount）”到某个载体线程上执行。

你可以把执行关系想象成：

- 平台线程：`Java Thread` <-> `OS Thread`
- 虚拟线程：`Virtual Thread` <->（动态挂载到）`Carrier (Platform) Thread` <-> `OS Thread`

**最重要的一点：虚拟线程遇到可管理的阻塞时，可以“卸载（unmount）”。**

- 当虚拟线程因为 `sleep`、很多 JDK 内置 IO、`LockSupport.park` 等进入阻塞等待时，JVM 能把它的执行状态“保存起来”，把载体线程释放出来去跑别的虚拟线程。
- 结果就是：**阻塞不再长期占用 OS 线程**，高并发下吞吐与资源利用会更好。

---

# 3. 虚拟线程能解决什么、不能解决什么 #

## 3.1 适合：阻塞密集型任务 ##

典型例子：

- 等数据库、等 RPC、等文件/网络 IO
- 大量短任务，大部分时间在等待
- 高并发“连接数/任务数”远大于 CPU 核心数

这类场景最常见的瓶颈不是 CPU，而是“等待”。虚拟线程能让等待更便宜。

## 3.2 不适合：CPU 密集型任务 ##

CPU 密集型（例如压缩/加密/大规模计算）真正受限的是 CPU 核心数：

- 你开再多虚拟线程，CPU 还是那几个核。
- 线程太多反而增加调度开销。

结论：**CPU 密集型仍然要限并行度**（固定线程池、ForkJoinPool、批处理分片、限流等）。

## 3.3 仍然要做“资源限流” ##

虚拟线程不会把外部资源变成无限：

- 数据库连接池只有几十/几百
- 下游接口 QPS 有限制
- 文件句柄、端口、带宽都有限

所以常见正确搭配是：

- 虚拟线程负责“便宜地等待”
- `Semaphore` / 限流 / 连接池负责“控制外部资源压力”

---

# 4. 如何创建与使用虚拟线程（JDK 21） #

## 4.1 直接创建：Thread API ##

三种常见写法：

```java
Thread.ofVirtual().start(() -> {
    // do work
});

Thread.startVirtualThread(() -> {
    // do work
});

Thread vt = Thread.ofVirtual().name("vt-", 0).unstarted(() -> {
    // do work
});
vt.start();
```

说明：

- 虚拟线程同样是 `Thread`，因此你熟悉的 `interrupt()`、`join()` 等语义仍然适用。
- 推荐给虚拟线程设置名字前缀，排查问题更容易（日志、线程 dump）。

## 4.2 工程上更常用：一任务一虚拟线程的 Executor ##

虚拟线程的“黄金用法”是：把每个任务当作独立工作单元，提交给“每任务一个虚拟线程”的执行器：

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
    exec.submit(() -> {
        // blocking IO is fine
        return 42;
    });
}
```

好处：

- 接口与线程池一致（`submit/execute`），迁移成本低。
- 不需要自己管理“虚拟线程池大小”，每个任务就是一个虚拟线程。
- `try-with-resources` 用完即关（JDK 21 的 `ExecutorService` 支持 `close()`，等价于 `shutdown()`）。

---

# 5. “阻塞”在虚拟线程里到底发生了什么？ #

你需要形成一个关键直觉：

> 虚拟线程的优势不是“跑得更快”，而是“**等待更便宜**”。

当虚拟线程在执行过程中需要等待（sleep/IO/park/lock 等）：

- **能卸载（unmount）的阻塞**：虚拟线程挂起，载体线程被释放。
- **不能卸载的阻塞（被钉住 pinned）**：载体线程也会一起被卡住，虚拟线程优势大打折扣。

下面重点解释“pinned”。

---

# 6. 必须掌握的坑：Pinned（钉住载体线程） #

虚拟线程本质上依赖“卸载”来避免占用载体线程。但有些情况下，JVM 不能把虚拟线程卸载，只能让它继续占着载体线程阻塞，这就叫 **pinned**。

你可以先记住一个最常见也最实用的经验法则：

> **不要在 `synchronized` 保护的临界区里做可能阻塞很久的事情（IO / sleep / 等锁）。**

因为当虚拟线程持有监视器（monitor，也就是 `synchronized` 的那把锁）时发生阻塞，容易出现 pinned。

## 6.1 典型反例：synchronized + 阻塞 ##

```java
synchronized (lock) {
    // 这里如果发生长时间阻塞（例如 IO、sleep），就可能 pin 住载体线程
    Thread.sleep(1000);
}
```

更好的做法：

- 缩小临界区：先拿/改共享状态，再在锁外执行阻塞操作。
- 如果必须“锁 + 等待”，优先使用 `java.util.concurrent` 的锁/同步器，并控制等待策略。

## 6.2 怎么发现 pinned：tracePinnedThreads ##

排查时最简单有效的手段之一是 JVM 参数：

- `-Djdk.tracePinnedThreads=short`
- `-Djdk.tracePinnedThreads=full`

它会在发生 pinned 时打印堆栈，直接告诉你“是谁在什么地方把载体线程钉住了”。

---

# 7. 中断、取消、ThreadLocal：行为上有哪些需要重新校准？ #

## 7.1 中断与取消：语义不变，但要“用对姿势” ##

虚拟线程依然遵守 Java 中断语义：

- `Thread.interrupt()`：发出中断信号。
- 可中断阻塞点（例如 `sleep`、很多 IO 等）会抛 `InterruptedException` 或提前返回。
- 任务执行器上用 `Future.cancel(true)` 触发中断是常见做法。

你仍然要做到：

- `catch (InterruptedException e) { Thread.currentThread().interrupt(); }`
- 不吞中断，不把中断当作普通异常处理掉。

## 7.2 ThreadLocal：能用，但别滥用 ##

虚拟线程数量可能非常多，如果你在每个线程里挂大量 `ThreadLocal`，内存与生命周期管理会变复杂。

建议：

- 能显式传参就显式传参
- 需要“跨调用链上下文”时，了解一下 JDK 21 的 `ScopedValue`（预览特性，需要 `--enable-preview`）

---

# 8. 线程池策略怎么变？从“控制线程数”变成“控制资源” #

过去我们习惯用固定线程池来“限制并发”，因为线程很贵：

- 线程池大小 ≈ 并发上限
- 队列长度 ≈ 排队上限

有了虚拟线程后，很多阻塞型任务可以回到更直观的模型：

> **一任务一线程（虚拟线程）**，并把“限制并发”的焦点转移到真正稀缺的资源上（连接池、下游配额、令牌桶等）。

换句话说：

- 虚拟线程解决的是“线程作为等待容器太贵”的问题。
- 你仍然需要解决“外部资源有限”的问题。

---

# 9. 最小可运行实验（推荐先跑） #

本仓库已经提供了配套实验：`labs/28-virtual-threads`。

## 9.1 对比阻塞任务吞吐 ##

```bash
cd labs/28-virtual-threads
javac *.java
java VirtualThreadsBlockingIoDemo 2000 50 64
```

你应该关注：

- `platform elapsedMs` vs `virtual elapsedMs`
- 改变 `poolSize`（第三个参数）对平台线程的影响

## 9.2 对比“创建很多线程”的成本 ##

```bash
cd labs/28-virtual-threads
javac *.java
java VirtualThreadCreationDemo 2000
```

你应该关注：

- 平台线程创建/等待在高数量下会更吃资源
- 虚拟线程在这种“短任务 + 等待”模式下更轻量

---

# 10. 常见误区（踩一次就很痛） #

1. **“虚拟线程 = 更快”**：不对。它主要让“等待”更便宜，不会让 CPU 计算更快。
2. **“有虚拟线程就不需要限流/连接池了”**：不对。外部资源仍然稀缺，虚拟线程只是不再把“线程数”当作稀缺资源。
3. **“随便 synchronized + 阻塞也没事”**：不对。可能出现 pinned，虚拟线程优势会被抵消。
4. **“虚拟线程可以无限创建”**：不对。数量上去后仍有内存、调度与对象管理成本。

---

# 11. 小结 #

- 虚拟线程让“同步阻塞式写法”在高并发阻塞场景下重新具备可扩展性。
- 适合阻塞密集型任务，不适合把 CPU 密集型当成“开更多线程就会更快”。
- 迁移心智：从“限制线程数”转向“限制真正稀缺的资源”。
- 警惕 pinned（尤其是 `synchronized` 临界区内阻塞），必要时用 `-Djdk.tracePinnedThreads` 辅助排查。
