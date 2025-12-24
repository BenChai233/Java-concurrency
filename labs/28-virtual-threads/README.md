# 第 28 章实验：虚拟线程（JDK 21） #

本章目标：用最小实验把“虚拟线程让等待更便宜”的直觉跑出来，并对比平台线程在线程池限制下的表现。

## 编译

先确认你使用的是 JDK 21：

```powershell
java -version
```

```powershell
cd labs\28-virtual-threads
javac *.java
```

## 实验 A：阻塞任务吞吐对比（`VirtualThreadsBlockingIoDemo`）

参数：`[tasks] [sleepMs] [poolSize]`

- tasks：任务数（默认 2000）
- sleepMs：每个任务阻塞时间（默认 50ms）
- poolSize：平台线程池大小（默认 64）

建议先跑 3 组对比（每组可重复 3 次取中位数）：

1) 默认参数：

```powershell
java VirtualThreadsBlockingIoDemo
```

2) 平台线程池偏小（更容易看到排队/等待的影响）：

```powershell
java VirtualThreadsBlockingIoDemo 2000 50 32
```

3) 拉长阻塞时间（让“等待成本”更突出）：

```powershell
java VirtualThreadsBlockingIoDemo 2000 200 64
```

观察点：

- `platform elapsedMs` vs `virtual elapsedMs` 的差距（重点看趋势，不要只看一次结果）
- `poolSize` 变小时，平台线程耗时是否明显变大
- 两种执行模型下任务是否都能完成（本 demo 用 latch 保证完成数一致）

## 实验 B：大量线程创建/等待对比（`VirtualThreadCreationDemo`）

参数：`[threads]`（默认 2000）

```powershell
java VirtualThreadCreationDemo
```

可选：加大数量（视机器资源而定）：

```powershell
java VirtualThreadCreationDemo 10000
```

观察点：

- `platform elapsedMs` 与 `virtual elapsedMs` 的差异
- 当 threads 变大时，平台线程更容易受内存/调度影响，甚至出现更明显的抖动

## 复盘模板（建议写在笔记里）

- 本章一句话：虚拟线程主要解决了什么问题？它为什么更适合“阻塞密集型任务”？
- 边界条件：CPU 密集任务为什么仍然要限制并行度？
- 线程池心智：有了虚拟线程后，“限制线程数”与“限制外部资源（连接池/下游配额）”的关注点怎么变化？
- 踩坑提醒：哪些阻塞/锁用法可能导致 pinned，从而抵消虚拟线程优势？
