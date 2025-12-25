# 第 32 章实验：Reactor 学习铺垫（用 JDK Flow 跑通 Reactive Streams）

本实验不引入 `reactor-core` 依赖，目的是先把 Reactor 的底层协议（Reactive Streams）与关键直觉跑出来：**操作符链 = Processor 链**、**背压 = request(n) 许可**、**线程切换的本质**。

## 编译

```powershell
java -version
cd labs\32-reactor-foundation
javac *.java
```

## 实验 A：操作符链与背压穿透（`FlowOperatorChainDemo`）

```powershell
cd labs\32-reactor-foundation
javac *.java
java FlowOperatorChainDemo
```

观察点：

- 输出中 `request(n)` 的节奏是否由最下游的“慢消费者”决定。
- `MapProcessor` 这一层是否只是“变换”而不会打破背压语义（1:1 映射时，下游 request(n) 应等量传递给上游）。
- `thread=...`：链路回调在哪个线程发生（默认 `SubmissionPublisher` 通常走 `ForkJoinPool.commonPool()`）。

## 实验 B：在 Subscriber 内部做线程切换（`FlowOffloadSubscriberDemo`）

```powershell
cd labs\32-reactor-foundation
javac *.java
java FlowOffloadSubscriberDemo
```

观察点：

- `publisherThread=...` vs `workerThread=...`：消费逻辑是否被切到专用线程执行。
- 只有当 worker 处理完并调用 `request(1)` 后，上游才会继续推送（背压仍然生效）。
- 取消（`cancel()`）后是否还能继续收到 `onNext`（通常不会）。

## 复盘模板（建议写在笔记里）

- 本章一句话：Reactor 的核心抽象与价值是什么？
- 背压的本质：`request(n)` 到底控制了什么？和“队列缓冲”有什么区别？
- 操作符链心智：你如何把 `map/filter/flatMap` 类比成 `Processor` 链？
- 线程模型：`publishOn/subscribeOn` 分别想解决什么问题？你如何避免在事件线程里阻塞？
- 工程边界：你的业务更适合 Reactor，还是更适合虚拟线程/CompletableFuture？判断依据是什么？

