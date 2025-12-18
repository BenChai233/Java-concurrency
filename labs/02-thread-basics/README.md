# Lab 02：线程状态与基本操作（JDK 21）

本实验对应第 2 章“线程的状态转换以及基本操作”，用最小可运行代码把以下现象变成“可观察”：

- `Thread.State`：`NEW/RUNNABLE/BLOCKED/WAITING/TIMED_WAITING/TERMINATED`
- 中断：打断 `sleep` vs 打断忙循环（中断标记如何变化）
- `join/sleep/yield/daemon`：超时等待、调度提示、守护线程退出行为

## 环境

- JDK：21+
- 无 Maven/Gradle，直接 `javac`/`java`

## 运行方式（Windows PowerShell）

```powershell
cd labs\02-thread-basics
javac *.java
```

### 1) 观察线程状态（重点）

```powershell
java ThreadStateTransitionsDemo
```

你应该能在输出里看到每个小 demo 的 `Observed: [...]` 集合里包含预期状态：

- `sleep` -> `TIMED_WAITING`
- `join()` -> `WAITING`
- `synchronized` 竞争 -> `BLOCKED`
- `Object.wait()` -> `WAITING`

如果某次没采样到（小概率，受机器负载影响），直接重跑一次即可。

### 2) 观察中断行为

```powershell
java InterruptScenariosDemo
```

关注点：

- `InterruptedException` 抛出时会清除中断标记（`isInterrupted` 变回 `false`）
- 忙循环不会自动停：需要你自己在循环里检查 `isInterrupted()` 并退出
- `Thread.interrupted()` 会“读取并清除”当前线程的中断标记

### 3) join / yield / daemon

join 超时等待（观察 joiner 的状态通常会是 `TIMED_WAITING`）：

```powershell
java JoinSleepYieldDaemonDemo --mode join-timeout
```

yield（多跑几次看波动）：

```powershell
java JoinSleepYieldDaemonDemo --mode yield
```

daemon（对比 daemon=true/false；daemon=false 时进程会一直活着，需要手动 Ctrl+C）：

```powershell
java JoinSleepYieldDaemonDemo --mode daemon --daemon true
java JoinSleepYieldDaemonDemo --mode daemon --daemon false
```

## 复盘建议

- 把“状态名”映射回“触发它的 API/语义”：`sleep/join/wait/synchronized` 分别对应什么等待/阻塞
- 记住中断的本质：它不是“强制杀线程”，而是“协作式通知 + 约定俗成的响应方式”

