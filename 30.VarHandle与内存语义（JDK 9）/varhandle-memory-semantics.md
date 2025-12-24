# 30. VarHandle 与内存语义（JDK 9） #

> 目标：把 VarHandle 的“用在哪里、比 Atomic* 多了什么能力、访问模式如何映射到内存语义”讲清楚，并能跑通本仓库 `labs/30-varhandle` 观察到 CAS 与 acquire/release 的效果。

---

# 1. 先讲动机：为什么会有 VarHandle？ #

在 JDK 9 之前，如果你需要：

- 对某个字段做 CAS（Compare-And-Set）
- 做一些“比 volatile 更细/更弱”的内存序控制（acquire/release/opaque）
- 写高性能并发数据结构

常见选项是：

- `AtomicInteger/AtomicReference/...`：好用、安全、可读性高，但粒度固定。
  - JDK 9+ 虽然也提供了 `getAcquire/setRelease/...` 之类的方法，但仍然是“包装类”思路。
- `sun.misc.Unsafe`：能力强，但不安全、不稳定、可维护性差。

**VarHandle** 的定位是：

> 在语言与库层面提供一种“可控、可读、标准化”的字段/数组元素原子访问方式，用来替代大部分 Unsafe 相关场景。

你可以把 VarHandle 看成“一个字段的多种访问门”。同一个字段，你可以选择：

- 普通读写
- 带 volatile 语义的读写
- acquire/release 语义的读写
- CAS/原子更新等

---

# 2. VarHandle 是什么：把它当成“字段句柄” #

VarHandle 是对“某个变量位置”的引用（句柄）。这个“变量位置”可以是：

- 对象字段（instance field）
- 静态字段（static field）
- 数组元素（array element）

获取 VarHandle 的常见方式是 `MethodHandles.lookup()`：

```java
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

class Counter { volatile int value; }

VarHandle VALUE = MethodHandles.lookup()
    .findVarHandle(Counter.class, "value", int.class);
```

拿到 VarHandle 之后，你就可以在这个字段上做各种访问：

- `getOpaque/getAcquire/getVolatile/...`
- `setOpaque/setRelease/setVolatile/...`
- `compareAndSet/getAndSet/...`

---

# 3. 访问模式（Memory Semantics）怎么理解？ #

你在 VarHandle 上选的访问模式，本质是在选择“编译器/CPU 能不能重排序、读写是否具有可见性保证”。

从强到弱大致是：

1. `volatile`：最强（对该变量的读写具备 volatile 语义）
2. `acquire/release`：中等强度（单向有序）
3. `opaque`：比 plain 稍强（保证“最终会看到”，但顺序更弱）
4. `plain`：最弱（普通读写，不额外提供并发语义）

一个非常实用的心智模型：

- **Release 写**：把“我之前做过的写入”推出去（发布）。
- **Acquire 读**：把“别人发布的写入”拉进来（获取）。

因此 acquire/release 常用来实现“发布-订阅（publication）”协议：

- 生产者先写数据，再 `setRelease(flag, 1)` 发布“数据准备好了”。
- 消费者先 `getAcquire(flag)` 看到发布，再去读数据。

这可以在很多场景下替代“全量 volatile”的开销，但它要求你有明确协议，并严格遵守。

---

# 4. 两个典型用法：CAS 与发布（acquire/release） #

## 4.1 CAS：原子更新计数器（或状态） ##

CAS 的关键点是：

- 你读取旧值（prev）
- 期望它仍然是 prev
- 把它更新为 prev+1
- 如果期间被别人改了，重试

伪代码：

```java
int prev;
do {
    prev = (int) VH.getVolatile(obj);
} while (!VH.compareAndSet(obj, prev, prev + 1));
```

说明：这里的 `getVolatile` 不是唯一选择，但在“计数器”这种场景下最直观，语义也最不容易踩坑。

## 4.2 发布：用 release/acquire 建立单向可见性 ##

经典模式是“写 data，再发布 flag”：

```java
DATA.setOpaque(box, data);
FLAG.setRelease(box, 1); // publish

while ((int) FLAG.getAcquire(box) == 0) { Thread.onSpinWait(); }
int value = (int) DATA.getOpaque(box); // consume
```

这里的关键不是 `DATA` 用了 opaque 还是 plain，而是：

- 发布必须是 `setRelease`
- 获取必须是 `getAcquire`
- 数据读写要围绕这个“发布点/获取点”形成协议

---

# 5. 最小可运行实验（推荐先跑） #

本仓库配套实验：`labs/30-varhandle`。

## 5.1 CAS 计数器：验证原子性 #

```bash
cd labs/30-varhandle
javac *.java
java VarHandleAtomicDemo 4 100000
```

观察点：

- `expected=... actual=...` 是否稳定相等
- 把线程数、迭代次数加大后是否仍正确（以及耗时变化）

## 5.2 acquire/release：体验发布-获取关系 #

```bash
cd labs/30-varhandle
javac *.java
java VarHandleAcquireReleaseDemo 100000
```

观察点：

- 程序是否能稳定结束（没有卡死）
- `lastObserved=...` 是否能合理变化（重点是理解协议而不是追求某个固定数值）

---

# 6. 什么时候该用 VarHandle？ #

优先级建议（非常工程化）：

1. 日常并发：优先 `Atomic*` / `synchronized` / `Lock` / 并发容器（可读性与正确性更重要）。
2. 需要更细内存语义、写并发组件/数据结构：考虑 VarHandle。
3. 除非你在写 JVM/基础设施或必须兼容历史代码：尽量避免 Unsafe。

---

# 7. 常见误区（尤其是“混用语义”） #

1. **把 VarHandle 当成“更酷的 Atomic*”到处用**：会显著降低可读性，维护成本更高。
2. **随意混用访问模式**：acquire/release/opaque/plain 混起来但没有清晰协议，容易产生数据竞争与诡异 bug。
3. **以为 setOpaque 等价 volatile**：不等价。opaque 更弱，不能替代“需要立即可见 + 有序”的场景。
4. **忘了并发协议不是 API 组合**：内存语义要服务于一个清晰的“发布/读取/状态机”设计，否则再强的 API 也救不了。

---

# 8. 小结 #

- VarHandle 是对字段/数组元素的标准化“句柄式访问”，覆盖原子操作与多种内存语义。
- acquire/release 常用于“发布-获取”协议；volatile 是更强、更容易正确的选择（但可能更贵）。
- 工程上默认优先 Atomic*；只有在你明确需要更细语义或在写底层组件时才上 VarHandle。
