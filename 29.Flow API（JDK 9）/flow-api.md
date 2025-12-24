# 29. Flow API（JDK 9） #

> 目标：把 Flow（Reactive Streams）这套“异步流 + 背压”模型讲清楚：为什么需要它、四个角色怎么协作、`request(n)`/`cancel()` 到底在控制什么，并能跑通本仓库 `labs/29-flow`。

---

# 1. 先讲动机：为什么需要 Flow（Reactive Streams）？ #

我们在并发里经常遇到“生产速度”和“消费能力”不匹配：

- 生产者很快（推数据很猛）
- 消费者很慢（处理耗时、IO、受限资源）

如果不做控制，就会出现：

- 队列堆积导致内存膨胀（OutOfMemory）
- 延迟越来越大（排队变长）
- 上下游相互拖垮（级联故障）

**Reactive Streams 的核心答案**是：

> 让消费者明确表达“我能处理多少（n）”，生产者必须尊重这个需求，这就是“背压（Backpressure）”。

Java 9 在 `java.util.concurrent.Flow` 里提供了 Reactive Streams 的标准接口（不是一个“全功能框架”，而是接口规范 + 一个简单实现）。

---

# 2. 四个角色：Publisher / Subscriber / Subscription / Processor #

Flow 里最关键的是“协作协议”，而不是“某个神奇类”。四个角色分别是：

- `Publisher<T>`：发布者，提供 `subscribe(Subscriber)`。
- `Subscriber<T>`：订阅者，接收数据与终止信号。
- `Subscription`：订阅关系的“控制器”，由 `Publisher` 交给 `Subscriber`，用它来 `request(n)`/`cancel()`。
- `Processor<T,R>`：既是 `Subscriber<T>` 又是 `Publisher<R>`，用于“中间处理/变换”。（本仓库实验主要用前三个即可）

Subscriber 的生命周期回调（按语义顺序）通常是：

1. `onSubscribe(subscription)`：建立订阅，拿到控制器（非常关键）。
2. 多次 `onNext(item)`：收到数据。
3. 终止信号（二选一且最多一次）：`onError(ex)` 或 `onComplete()`。

---

# 3. 背压的核心：request(n) 是“许可证”，不是“加速键” #

Flow 是“以消费者为中心”的节奏控制：

- **没有 `request(n)`，Publisher 不应调用 `onNext`。**
- `request(n)` 表示：“从现在开始，我最多还能再处理 n 个 `onNext`”。
- `cancel()` 表示：“我不想要了，停止推送，释放资源”。

你可以把 `request(n)` 想成“消费许可证（permit）”：

- 消费者每处理完一个，再申请一个（`request(1)`）——最容易理解，也最安全。
- 或者一次申请一批（`request(1024)`），减少交互次数，提高吞吐（但需要更小心内存与延迟）。

## 3.1 最小正确姿势：onSubscribe 里先 request #

```java
public void onSubscribe(Flow.Subscription s) {
    this.subscription = s;
    s.request(1); // 不 request，后面很可能永远没有 onNext
}
```

## 3.2 onNext 里“处理完再申请下一批” #

```java
public void onNext(T item) {
    handle(item);
    subscription.request(1);
}
```

注意：`request(n)` 并不是“我要立刻拿到 n 个”，而是“允许最多 n 个”。具体推送节奏取决于实现。

---

# 4. SubmissionPublisher：一个足够用来学习的 Publisher #

JDK 自带了一个简单实现：`java.util.concurrent.SubmissionPublisher`。

它适合用来：

- 体验 `subscribe`/`request`/`cancel`/`close`
- 观察回调在哪个线程执行
- 写最小实验验证背压语义

但也要知道它的定位：**学习与简单场景**，不是替代 Reactor/RxJava 的完整方案。

---

# 5. 线程与执行：onNext 在哪个线程跑？ #

Flow/Reactive Streams 不强制要求“回调一定在哪个线程”。以 `SubmissionPublisher` 为例：

- 它内部会使用执行器把数据分发给订阅者。
- 如果你不传 `Executor`，它会使用默认执行器（通常是 `ForkJoinPool.commonPool()`）。

这会带来一个实践要点：

> **不要在 `onNext` 里做长时间阻塞**（sleep/IO/等待锁），否则可能拖慢分发线程，影响整体吞吐。

如果确实要阻塞：

- 把阻塞工作丢到独立线程池 / 虚拟线程执行器，再把结果异步回传（这就进入更完整的 Reactive/异步设计了）。

---

# 6. 如何正确终止：close、onComplete、onError、cancel #

终止有两类：

- 正常结束：Publisher `close()` -> Subscriber `onComplete()`
- 异常结束：Publisher 触发错误 -> Subscriber `onError(ex)`

Subscriber 也可以主动取消：

- `subscription.cancel()`：告诉 Publisher “别再发了”，并尽快释放资源

经验法则：

- “我不再需要数据了”用 `cancel()`。
- “数据源自然结束了”用 `close()`/`onComplete()`。
- “出现不可恢复错误”用 `onError()`。

---

# 7. 最小可运行实验（推荐先跑） #

本仓库配套实验：`labs/29-flow`。

## 7.1 背压：request(1) 如何控制节奏 #

```bash
cd labs/29-flow
javac *.java
java FlowBackpressureDemo
```

观察点：

- `onNext=... thread=...`：回调线程是谁？
- `Thread.sleep(50)`（消费者变慢）时，是否仍然稳定地“一个处理完再来一个”？

## 7.2 取消：cancel 之后是否还会继续 onNext #

```bash
cd labs/29-flow
javac *.java
java FlowCancelDemo
```

观察点：

- 收到 5 个后 `cancel()`，是否还会继续打印 `onNext`？
- 取消后 Publisher 的 `close()` 是否还会触发 `onComplete`（取决于实现与时序，重点是理解“取消优先停止数据流”）。

---

# 8. 常见误区（比 API 更重要） #

1. **忘记 request**：没有 `request(n)`，就没有 `onNext`（这是协议要求）。
2. **在 onNext 里阻塞很久**：会拖慢分发线程，造成“看起来像背压失效/吞吐下降”。
3. **把背压当成队列**：背压是节奏控制协议，不是“无限缓存”。缓存还是要付出内存成本。
4. **忽视取消与资源释放**：长连接/长流场景里，不 cancel 可能导致资源泄漏。

---

# 9. 小结 #

- Flow 是 Reactive Streams 的标准接口，核心价值是背压协议。
- `Subscription.request(n)` 是“最多允许 n 个 onNext”的消费许可证；不 request 就不会流动。
- `cancel()` 用于提前停止数据流并释放资源，`close()` 用于正常结束。
- `SubmissionPublisher` 适合学习与小实验：先把协议跑通，再谈更完整的框架与工程落地。
