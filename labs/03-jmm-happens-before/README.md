# Lab 03：JMM 与 happens-before（JDK 21）

本实验对应第 3 章“Java 内存模型以及 happens-before 规则”，用两个最小可运行演示将把概念落到可观察的结果上：

- `volatile` 可见性与非 `volatile` 对比（消息传递模型）
- `start/join` 的 happens-before 保证

## 环境

- JDK 21+
- 无 Maven/Gradle，直接使用 `javac`/`java`

## 运行方式（Windows PowerShell）

```powershell
cd labs\03-jmm-happens-before
javac *.java
```

### 1) `volatile` 可见性：消息传递

```powershell
java VolatileVisibilityDemo --iterations 10000 --timeoutMs 50 --useVolatile true
java VolatileVisibilityDemo --iterations 10000 --timeoutMs 50 --useVolatile false
```

观察点：

- 输出包含参数、timeouts、staleReads、elapsedMs
- `useVolatile=true` 时应该保持 `timeouts=0` 且 `staleReads=0`
- `useVolatile=false` 可能出现超时或脏读；如果没出现，加大 `--iterations` 或 `--timeoutMs` 再跑

### 2) `start/join` 的 happens-before

```powershell
java StartJoinHappensBeforeDemo --iterations 1000000
```

观察点：

- `wrongReads` 应该始终为 0
- `join` 建立 happens-before，主线程必须可见写线程的结果

## 复盘建议

- `volatile` 的写->读建立 happens-before，保证可见性
- `start/join` 在无 `volatile` 的情况下也能保证可见性
- 非 `volatile` 不保证可见性，所以使用超时 + 统计避免死等
