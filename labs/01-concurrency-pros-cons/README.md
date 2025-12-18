# 第 1 章实验：并发编程的优缺点

本章目标：用最小实验把“并发什么时候更快/更慢”和“并发的典型风险”跑出来。

## 编译

先确认你使用的是 JDK 21：

```powershell
java -version
```

```powershell
cd labs\01-concurrency-pros-cons
javac *.java
```

## 实验 A：并发收益边界（`ConcurrencyBenefits`）

建议先跑 3 组对比（每组可以重复 3 次取中位数）：

1) CPU 密集（通常不会无限加速）

```powershell
java ConcurrencyBenefits --mode cpu --tasks 200 --threads 2 --work 4000000
java ConcurrencyBenefits --mode cpu --tasks 200 --threads 8 --work 4000000
```

2) 等待密集（sleep/IO 模拟；并发更容易受益）

```powershell
java ConcurrencyBenefits --mode io --tasks 200 --threads 8 --sleepMs 20
```

3) 等待密集 + 虚拟线程（JDK 21 特性）

```powershell
java ConcurrencyBenefits --mode io --tasks 200 --virtual true --sleepMs 20
```

记录你观察到的结论：什么时候变快、什么时候变慢、原因是什么（线程切换/锁/缓存/等待隐藏）。

## 实验 B：数据竞争与修复（`RaceConditionAndFixes`）

```powershell
java RaceConditionAndFixes --threads 8 --iterations 2000000
```

观察点：

- `broken` 结果是否小于 `expected`（丢失更新）
- `synchronized` / `atomic` / `longAdder` 是否正确，耗时差异如何

## 复盘模板（建议写在笔记里）

- 本章一句话：并发带来收益的前提是什么？
- 代价清单：线程创建/切换、锁竞争、缓存一致性、复杂度（调试/复现）
- 风险清单：数据竞争、可见性、死锁/活锁、资源耗尽
- 你机器上的结论：CPU 密集最优线程数大概是多少？等待密集任务用什么执行模型更合适？
