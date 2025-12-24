# 第 31 章实验：CompletableFuture（JDK 9+ 增强） #

本章目标：用 3 个小实验把 CompletableFuture 的“组合方式 + 超时/降级 + 延迟调度”跑出来，形成可落地的用法模板。

## 编译

先确认你使用的是 JDK 21：

```powershell
java -version
```

```powershell
cd labs\31-completablefuture
javac *.java
```

## 实验 A：三板斧对比（`CompletableFutureBasicsDemo`）

```powershell
java CompletableFutureBasicsDemo
```

观察点：

- `thenApply`：把值变换成新值（map）
- `thenCompose`：把“返回 future 的函数”扁平化（flatMap），避免 `CompletableFuture<CompletableFuture<T>>`
- `thenCombine`：两个 future 都完成后合并结果
- `exceptionally`：异常时如何回退到默认值
- `allOf`/`anyOf`：它们的返回类型与等待行为（尤其是 `anyOf` 返回 `Object`）

## 实验 B：超时与降级（`CompletableFutureTimeoutDemo`）

```powershell
java CompletableFutureTimeoutDemo
```

观察点：

- `orTimeout`：超时后以异常完成（你会看到 `timeout:...` 的输出）
- `completeOnTimeout`：超时后以默认值完成（直接得到 `default`）
- `failedFuture`：如何快速创建一个失败阶段并进入异常处理分支

## 实验 C：延迟调度（`CompletableFutureDelayedExecutorDemo`）

```powershell
java CompletableFutureDelayedExecutorDemo
```

观察点：

- `delayedExecutor` 延后提交任务（对比 `Thread.sleep`：它不会占用线程空转等待）
- 输出里的 `elapsedMs` 是否大致接近设置的延迟（存在机器调度误差）

## 复盘模板（建议写在笔记里）

- 本章一句话：CompletableFuture 的价值是什么？它在解决“回调地狱”还是在解决“组合与治理”（超时/降级/汇聚）？
- 组合选择题：什么时候用 apply / compose / combine？
- 超时策略：业务上超时应该“失败”还是“默认值降级”？分别适合什么场景？
- 执行器意识：默认线程池适合什么负载？阻塞任务为什么要谨慎（线程饥饿/吞吐下降）？
