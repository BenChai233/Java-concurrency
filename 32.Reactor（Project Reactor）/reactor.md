# 32. Reactor（Project Reactor）：响应式编程入门（Reactive Streams）

> 目标：用“能落地写业务”的方式掌握 Reactor 的核心概念与使用姿势：`Flux/Mono`、操作符链、背压（Backpressure）、线程切换（`publishOn/subscribeOn`）、错误处理与取消；并能跑通本仓库配套实验 `labs/32-reactor-foundation`，把 Reactor 的心智模型和 `Flow API` 对齐。

---

## 1. 先讲动机：为什么要学 Reactor（Reactive Programming）？

当你开始做这些事时，“同步阻塞 + 线程池”会越来越吃力：

- 上游事件/数据是“连续到达”的（消息、日志、监控、WebSocket、队列消费、流式 IO）。
- 下游处理能力波动很大（IO、限流、依赖抖动），需要**节奏控制**而不是“堆队列等死”。
- 你希望把一串异步步骤写成**可组合的流水线**（变换、过滤、合并、重试、超时、降级）。

Reactor 的核心价值可以概括成两句话：

1. 用 **Publisher/Subscriber** 模型把“数据流 + 终止信号 + 错误信号”统一起来。
2. 用 **Backpressure（背压）** 把“生产速度”和“消费能力”显式对齐，避免失控堆积。

如果你已经学过本仓库的 `29.Flow API（JDK 9）`：那一章讲的是**标准接口（协议）**；Reactor 讲的是**生产级实现 + 丰富操作符 + 调度器体系**。

---

## 2. 核心概念对齐：Reactive Streams 四角色 vs Reactor 的 Mono/Flux

Reactive Streams（JDK `Flow` 也是它的接口标准）四个角色：

- `Publisher<T>`：发布者，提供 `subscribe(Subscriber)`
- `Subscriber<T>`：订阅者，处理 `onNext/onError/onComplete`
- `Subscription`：订阅关系，提供 `request(n)` 和 `cancel()`
- `Processor<T, R>`：既是 Subscriber 又是 Publisher，用于“中间处理”

Reactor 的抽象更“业务化”：

- `Flux<T>`：0..N 个元素的异步序列（流）
- `Mono<T>`：0..1 个元素的异步序列（单值）

它们本质上仍然是 `Publisher`：你最终都会 `subscribe(...)`，并在订阅关系里体现 `request(n)`、取消、错误、完成。

---

## 3. 背压（Backpressure）：request(n) 是“许可”，不是“催促”

Reactor 的很多“看起来很高级”的能力，底层都绕不开这条协议：

- **没有 demand（没有 `request(n)`），上游就不该继续 `onNext`。**
- `request(n)` 表示：下游最多还能处理 n 个元素（许可额度）。
- `cancel()` 表示：下游明确不再需要更多元素，上游应尽快停止并释放资源。

工程直觉：

- “慢消费者”不是问题，**慢消费者 + 无背压**才会把系统拖死。
- 背压不是队列；队列只是缓冲，背压是“允许发送多少”的契约。

---

## 4. 基本用法：Flux/Mono 的最小心智模型（示意）

下面是典型的 Reactor 风格（示意，便于理解操作符链；本仓库默认不引入依赖）：

```java
Flux.range(1, 10)
    .map(i -> i * 2)
    .filter(i -> i % 3 != 0)
    .flatMap(i -> callRemote(i))       // 异步展开（并发）
    .timeout(Duration.ofSeconds(1))
    .retry(2)
    .publishOn(Schedulers.boundedElastic())
    .subscribe(
        v -> System.out.println(v),
        e -> e.printStackTrace(),
        () -> System.out.println("done")
    );
```

理解要点：

- 每个操作符都在“包装”上游：你可以把它类比成 `Flow.Processor` 的链式组合。
- 线程切换不是“免费午餐”：它影响吞吐、延迟、上下文切换成本，也会影响背压传播路径。

---

## 5. 线程与调度：subscribeOn vs publishOn（工程上最常踩坑的点）

你可以先记住一句话（更像口诀）：

- `subscribeOn`：影响“订阅发生在哪个线程”（通常也会影响上游产出线程）
- `publishOn`：影响“从这里开始，下游在哪个线程消费”

工程建议：

- 不要在默认事件线程（例如 Netty 的 event loop）里做阻塞 IO。
- 阻塞 IO 迁移到 `boundedElastic`（或你自己的隔离线程池/虚拟线程策略）。
- CPU 密集型工作避免无脑切换线程，优先控制并行度。

---

## 6. 错误处理：错误也是信号，且终止链路

Reactive Streams 的终止信号是互斥且最多一次：

- 正常结束：`onComplete()`
- 异常结束：`onError(Throwable)`

因此 Reactor 的错误处理要点是：

- 你需要在链路里明确“遇错怎么办”（替换默认值/重试/降级/吞掉/转化）。
- 订阅时如果不传 `onError`，很多场景会把异常交给全局处理器，排查更痛苦。

常用思路（示意）：

- `onErrorReturn(defaultValue)`
- `onErrorResume(ex -> fallbackPublisher)`
- `retryWhen(...)`
- `timeout(...)`

---

## 7. 适用边界：什么时候 Reactor 是好选择？

更适合 Reactor：

- IO 密集、事件驱动、需要控制并发与背压（消息消费、网关、流式处理）。
- 需要把异步步骤组合成可维护的 pipeline（重试、超时、并发合并）。

不一定适合 Reactor：

- 纯 CPU 密集型且逻辑很线性（可能普通并行流/线程池更直接）。
- 团队对响应式不熟，且场景不需要背压/流式组合（引入复杂度得不偿失）。

与虚拟线程的关系（JDK 21）：

- 虚拟线程让“阻塞式写法”更可扩展，但不等于替代 Reactor。
- Reactor 擅长“少线程处理海量连接 + 背压 + 流式组合”；虚拟线程擅长“同步直写 + 可扩展阻塞”。

---

## 8. 配套实验：用 JDK Flow 练 Reactor 的底层模型

本仓库配套实验：`labs/32-reactor-foundation`

- 实验 A：用 `Flow.Processor` 组装“操作符链”，观察背压如何穿透链路
- 实验 B：在 Subscriber 内部做“异步/线程切换”，观察 request(n) 与吞吐的关系

运行与观察点见实验目录 README。

---

## 9. 小结

- Reactor 的 `Flux/Mono` 本质是 `Publisher`，核心是“可组合的数据流 + 背压协议”。
- `request(n)` 是下游给上游的“处理许可”，`cancel()` 是明确停止的契约。
- 操作符链可以类比为一串 `Processor`，错误/完成都是终止信号且互斥。
- `subscribeOn/publishOn` 决定了链路的线程模型，是排查吞吐/延迟问题的关键入口。
- 本章实验先用 JDK `Flow` 把模型跑通，再迁移到 Reactor API 会轻松很多。

