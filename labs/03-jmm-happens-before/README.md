# Lab 03：JMM 与 happens-before（JDK 21） #

本章目标：用最小实验把 JMM 里最关键的两条“可见性保证”跑出来：

- `volatile` 写-读建立 happens-before（消息传递/发布）
- `start/join` 的 happens-before 保证

## 编译

先确认你使用的是 JDK 21：

```powershell
java -version
```

```powershell
cd labs\03-jmm-happens-before
javac *.java
```

## 实验 A：消息传递（Publication）与 `volatile` #

这是典型“发布-订阅”模型：写线程写入 `data`，再发布 `ready=true`；读线程看到 `ready=true` 后再读取 `data`。

### A1) 正确发布顺序（默认）：data -> ready（`VolatileVisibilityDemo`）

```powershell
java VolatileVisibilityDemo --mode publication --ordering correct --iterations 10000 --timeoutMs 50 --useVolatile true
java VolatileVisibilityDemo --mode publication --ordering correct --iterations 10000 --timeoutMs 50 --useVolatile false
```

观察点：

- 输出包含 `timeouts` 与 `staleReads`
- `useVolatile=true` 时应稳定 `timeouts=0` 且 `staleReads=0`
- `useVolatile=false` 在部分机器/JIT 下可能仍然是 `0/0`：这不代表“正确”，只是“没有被你这次跑出来”（数据竞争在 JMM 下本来就不保证可复现）

### A2) 故意写错顺序：ready -> data（稳定可观察 staleReads）

这组实验的目的不是“证明 volatile 没用”，而是强调：

> `volatile` 能保证可见性/有序性，但你仍然必须按正确顺序发布数据（先写 data，再写 volatile ready）。

```powershell
java VolatileVisibilityDemo --mode publication --ordering wrong --writerDelayMs 1 --iterations 200 --timeoutMs 50 --useVolatile true
```

观察点：

- `staleReads` 应显著大于 0（甚至等于 iterations）
- 解释原因：读线程可能先看到 `ready=true`，但此时 `data` 还没写入（逻辑层面的“先后顺序”错误）

## 实验 B：Stop-Flag 可见性（`VolatileVisibilityDemo`）

这是经典“停机标志”例子：写线程把 `stop=true`，读线程一直自旋等待 stop 变为 true。

```powershell
java VolatileVisibilityDemo --mode stopFlag --stopTrials 2000 --timeoutMs 200 --useVolatile true
java VolatileVisibilityDemo --mode stopFlag --stopTrials 2000 --timeoutMs 200 --useVolatile false
```

观察点：

- 输出 `stuck=...`：表示有多少次读线程在超时内没能观察到 stop 变更
- `useVolatile=false` 可能在你的机器上仍然 `stuck=0`，但它依然是数据竞争：JMM 不保证读线程能看到更新

## 实验 C：start/join 的 happens-before（`StartJoinHappensBeforeDemo`）

```powershell
java StartJoinHappensBeforeDemo --iterations 1000000
```

观察点：

- `wrongReads` 应该始终为 0
- `join` 建立 happens-before，主线程必须可见写线程的结果

## 复盘模板（建议写在笔记里）

- 本章一句话：JMM 里“可见性保证”来自哪里？volatile 与 join 分别在保证什么？
- Publication 协议：为什么必须 `data` 先写、`ready(volatile)` 后写？读侧为什么要先读 `ready` 再读 `data`？
- 复现心智：为什么 `useVolatile=false` 可能跑不出问题，但依然是不正确的并发代码？
- 工程习惯：遇到“偶现不复现”的并发 bug，你会如何最小化并验证（缩小模型/加统计/长时间跑/增加负载）？
