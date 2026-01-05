# 第 09 章实验：AQS 深入理解（09-aqs-deep-dive）

本实验对应第 09 章《深入理解 AbstractQueuedSynchronizer(AQS)》，用 3 个最小 Demo 帮你把“同步队列 / 公平性 / 条件队列”跑出来、看明白。

## 编译

```powershell
java -version
cd labs\09-aqs-deep-dive
javac *.java
```

## 实验 A：公平锁 vs 非公平锁的获取顺序（`FairVsNonfairLockDemo`）

```powershell
java FairVsNonfairLockDemo
# 参数：waiters bargers rounds
java FairVsNonfairLockDemo 6 2 10
```

观察点（该实验受调度影响，主要看趋势）：
- 实验设计：主线程先持锁，先启动 `W*`（waiters）进入排队等待；再启动 `B*`（bargers）并让它们卡在“解锁信号”上，保证它们在解锁瞬间参与竞争。
- 看 `acquire=[...]`：若 `B*` 出现在前 `waiters` 个位置，说明后来者在队列未清空时抢到了锁（插队/抢占）。
- 对 `fair`：通常更少出现 `bargerInFirstWaiters=true`（有排队前驱时倾向不插队）。
- 对 `nonfair`：更可能出现 `bargerInFirstWaiters=true`（允许插队）。

## 实验 B：用 AQS 实现最小互斥锁并校验互斥（`SimpleAqsMutexDemo`）

```powershell
java SimpleAqsMutexDemo
# 或指定 threads / loops
java SimpleAqsMutexDemo 4 200000
```

观察点：
- 输出 `expected=... actual=...` 应严格相等（互斥正确性校验）。
- `ms=...` 用于做粗略对比（不是严谨 benchmark）。
- `mutex locked=false` 表示最后锁已释放（否则说明 unlock 路径有问题）。

## 实验 C：条件队列 await/signal 的正确用法（`AqsConditionQueueDemo`）

```powershell
java AqsConditionQueueDemo
```

观察点：
- 未持有锁调用 `await()` 会打印：`await without lock throws: IllegalMonitorStateException`。
- 正确用法下会按序出现：`waiter awaiting` -> `signaling` -> `waiter resumed`。
- 把 `signal()` 改成不加锁调用（自行改代码）应同样抛 `IllegalMonitorStateException`（验证 signal 也要求持锁）。

## 复盘模板（建议写在笔记里）

- 本章一句话：AQS 用什么队列/协议把“抢锁失败的线程”组织起来并安全阻塞与唤醒？
- 公平/非公平的核心差异是什么？你从实验 A 的输出里看到了哪些“插队”迹象？
- AQS 的最小实现需要覆写哪些方法？哪些错误实现会导致“互斥失效/无法唤醒/非法释放”？
- `Condition.await/signal` 为什么必须在持锁时调用？条件队列与同步队列的关系是什么？
- 如果线上卡死，你会优先看哪些线索（线程 dump、持锁线程、队列节点状态等）？