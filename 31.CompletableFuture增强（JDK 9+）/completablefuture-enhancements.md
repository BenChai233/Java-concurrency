# 31. CompletableFuture 增强（JDK 9+） #

> 目标：用“能写业务”的方式掌握 CompletableFuture：先把 JDK 8 的组合模型讲清楚（阶段/线程/异常），再重点讲 JDK 9+ 的超时与延迟增强，最后能跑通本仓库 `labs/31-completablefuture`。

---

# 1. 先讲动机：为什么需要 CompletableFuture？ #

你可以把 CompletableFuture 看成两件事的结合：

- **Future**：代表“将来某个时刻会有结果（或失败）”。
- **Pipeline（流水线）**：结果出来后，自动触发下一步（变换/组合/异常处理），避免回调地狱。

它最适合的场景是：

- 并行调用多个下游，最后汇总
- 一步依赖一步的异步调用链（A -> B -> C）
- 需要统一处理异常、超时、降级

---

# 2. 核心心智模型：Stage（阶段）与 Completion（完成） #

CompletableFuture 的每一次 `thenXxx(...)` 都在创建一个新的“阶段（stage）”：

- 上游阶段完成（成功/失败）后，触发下游阶段。
- 下游阶段可以：
  - 变换值（map）
  - 扁平化（flatMap）
  - 合并多个阶段
  - 处理异常

你要牢牢记住两点：

1. **完成（completion）只发生一次**：成功就是一个值，失败就是一个异常。
2. **不要在阶段里做阻塞等待**：尤其是 `join()`/`get()`，否则很容易把异步写回同步、甚至造成线程饥饿。

---

# 3. JDK 8 基础用法：先把“组合”三板斧练熟 #

## 3.1 创建：runAsync / supplyAsync ##

```java
CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> work());
CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> 42);
```

默认执行器通常是 `ForkJoinPool.commonPool()`，它更偏向 CPU 密集型任务。如果你的任务会阻塞（IO、sleep、等锁），应考虑：

- 显式传入自定义 `Executor`（线程池）
- 或在虚拟线程章节的思路下，用虚拟线程执行器承载阻塞（见第 6 节）

## 3.2 变换（map）：thenApply vs thenApplyAsync ##

- `thenApply(fn)`：在“触发完成的线程”上继续执行（可能是上游线程）。
- `thenApplyAsync(fn)`：把执行切换到默认/指定执行器。

工程建议：

- 不确定线程来源、且阶段逻辑可能较重时，倾向 `thenApplyAsync(..., executor)` 显式指定执行器。

## 3.3 扁平化（flatMap）：thenCompose ##

当你的函数返回的本身就是 CompletableFuture（例如调用下游异步接口）：

```java
CompletableFuture<User> f =
    loadUserId()
        .thenCompose(id -> fetchUserAsync(id)); // 关键：compose 扁平化
```

记住一句话：

> `thenApply` 会得到 `CompletableFuture<CompletableFuture<T>>`，`thenCompose` 才是你想要的 `CompletableFuture<T>`。

## 3.4 合并与汇聚：thenCombine / allOf / anyOf ##

- `thenCombine`：等两个都成功后合并结果（任一失败则失败）。
- `allOf`：等全部完成（返回 `CompletableFuture<Void>`，需要你自己取结果）。
- `anyOf`：任一完成即完成（返回 `CompletableFuture<Object>`）。

实用建议：

- `anyOf` 的 `Object` 是“设计如此”，你通常需要自己做类型约束（例如只放同类型 future）。

## 3.5 异常：exceptionally / handle / whenComplete ##

- `exceptionally(ex -> fallback)`：失败时给替代值（只处理异常路径）。
- `handle((v, ex) -> ...)`：成功/失败都进来（可返回新值）。
- `whenComplete((v, ex) -> ...)`：只做收尾/日志，不改变结果（除非你另行 complete）。

---

# 4. JDK 9+ 增强：把“超时+降级”写短写清楚 #

JDK 8 做超时一般要自己再建一个“超时 future”去 compete，非常模板化。JDK 9+ 增强让这件事一行搞定。

## 4.1 orTimeout：超时就失败 ##

```java
CompletableFuture<String> r =
    slowCall()
        .orTimeout(50, TimeUnit.MILLISECONDS);
```

它超时后会以异常完成（通常是 `TimeoutException`），你可以接 `exceptionally/handle` 做降级。

## 4.2 completeOnTimeout：超时给默认值 ##

```java
CompletableFuture<String> r =
    slowCall()
        .completeOnTimeout("fallback", 50, TimeUnit.MILLISECONDS);
```

它的语义更像“超时自动降级”，不需要走异常通道。

## 4.3 failedFuture：快速造一个失败阶段 ##

```java
CompletableFuture<String> bad =
    CompletableFuture.failedFuture(new IllegalStateException("boom"));
```

在“校验失败直接短路”“并行汇聚中人为制造失败”时很实用。

## 4.4 delayedExecutor：延迟调度（不等于 sleep） ##

```java
Executor delayed = CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS);
CompletableFuture<String> r = CompletableFuture.supplyAsync(() -> "hello", delayed);
```

它的意义是“把任务提交延后”，而不是占用线程去 sleep 等待。

---

# 5. 线程与执行器：不要让默认线程池替你背锅 #

你需要明确两类负载：

- **CPU 密集**：默认 `commonPool` 往往没问题。
- **阻塞密集（IO/waiting-heavy）**：默认线程池可能被阻塞占满，导致回调无法执行，出现吞吐骤降甚至“看起来像死锁”的饥饿问题。

工程建议（简化版）：

- 阻塞任务：显式传入业务线程池（或者使用虚拟线程执行器）。
- CPU 密集：固定并行度，避免创建过多线程。

---

# 6. 与虚拟线程的组合（JDK 21 思路）：阻塞任务也能写得很直 #

当你的异步链路里不可避免要做阻塞（例如调用阻塞式 JDBC、阻塞式 HTTP 客户端），一种更“直”的方式是：

- 用虚拟线程承载阻塞
- 仍然用 CompletableFuture 做组合与超时/降级

示意（只强调思路）：

```java
try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
    CompletableFuture<String> r = CompletableFuture.supplyAsync(() -> blockingCall(), exec);
}
```

这并不是说“有了虚拟线程就可以无限并发”，外部资源（连接池/下游配额）仍然要限流。

---

# 7. 最小可运行实验（推荐先跑） #

本仓库配套实验：`labs/31-completablefuture`。

## 7.1 基础组合：apply/compose/combine + allOf/anyOf ##

```bash
cd labs/31-completablefuture
javac *.java
java CompletableFutureBasicsDemo
```

观察点：

- `thenApply` / `thenCompose` / `thenCombine` 的结果差异
- `exceptionally` 如何把失败转成默认值
- `anyOf` 为什么是 `Object`

## 7.2 超时：orTimeout vs completeOnTimeout ##

```bash
cd labs/31-completablefuture
javac *.java
java CompletableFutureTimeoutDemo
```

观察点：

- `orTimeout` 走异常路径（你会看到 `timeout:...`）
- `completeOnTimeout` 走默认值路径（直接得到 `default`）
- `failedFuture` 如何快速进入异常分支

## 7.3 延迟执行：delayedExecutor ##

```bash
cd labs/31-completablefuture
javac *.java
java CompletableFutureDelayedExecutorDemo
```

观察点：

- `elapsedMs` 是否接近你设置的延迟（注意机器调度误差）

---

# 8. 常见误区（90% 的 CompletableFuture 问题都在这） #

1. **在回调里 `join()/get()`**：把异步链路阻塞住，吞吐下降，甚至线程饥饿。
2. **不关心执行器来源**：默认线程池不一定适合业务负载，阻塞任务更容易把它“堵死”。
3. **混用同步/异步版本导致线程跳来跳去**：`thenApply`/`thenApplyAsync` 混用但不指定 executor，排查问题很痛苦。
4. **误用 anyOf**：忽视返回 `Object`，以及“谁先完成就用谁”的语义（可能导致非预期的胜出者）。

---

# 9. 小结 #

- CompletableFuture 是“Future + 可组合阶段”的统一模型：变换（apply）、扁平化（compose）、合并（combine）是基础三板斧。
- JDK 9+ 把超时与延迟变成一等能力：`orTimeout/completeOnTimeout/failedFuture/delayedExecutor` 能显著减少模板代码。
- 工程上最重要的是线程模型：阻塞任务要么给专用 executor，要么考虑虚拟线程承载阻塞，但外部资源仍需限流。
