# 第 29 章实验：Flow API（Reactive Streams） #

本章目标：用最小实验把 Flow 的“四角色 + 背压协议 + 取消语义”跑出来，形成对 `request(n)` 的正确直觉。

## 编译

先确认你使用的是 JDK 21：

```powershell
java -version
```

```powershell
cd labs\29-flow
javac *.java
```

## 实验 A：背压的最小闭环（`FlowBackpressureDemo`）

```powershell
java FlowBackpressureDemo
```

观察点：

- 没有 `request(n)` 时不会有 `onNext`（协议要求）
- `request(1)` 的模式下：处理完一个再申请一个，数据流动稳定可控
- `onNext` 的执行线程是谁（看输出里的 `thread=...`）

可选：打开 `FlowBackpressureDemo`，观察它在 `onSubscribe`/`onNext` 里分别做了什么（核心就是 `subscription.request(1)`）。

## 实验 B：取消语义（`FlowCancelDemo`）

```powershell
java FlowCancelDemo
```

观察点：

- 收到一定数量后 `cancel()`：后续是否还会继续收到 `onNext`
- 取消后 Publisher 是否还会走到 `close()`（取决于实现与时序，但重点是理解“取消会停止数据流并提示释放资源”）

## 复盘模板（建议写在笔记里）

- 本章一句话：Flow 背压的核心是什么？`request(n)` 到底在表达什么？
- 四角色关系：Publisher / Subscriber / Subscription / Processor 分别负责什么？
- 取消语义：`cancel()` 解决什么问题？如果不 cancel 可能出现什么风险（资源泄漏/无意义推送）？
- 工程注意点：为什么不建议在 `onNext` 里做长时间阻塞（IO/sleep）？
