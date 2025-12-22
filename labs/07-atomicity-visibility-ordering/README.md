# 07-atomicity-visibility-ordering

本实验为第 07 章（原子性/可见性/有序性总结）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java AtomicitySummaryDemo [threads] [loops]
   java VisibilitySummaryDemo
   java OrderingSummaryDemo [iterations]

## 实验说明

AtomicitySummaryDemo
- 目标：验证 ++ 不是原子操作。
- 观察：实际值小于期望值。

VisibilitySummaryDemo
- 目标：验证 volatile 的可见性。
- 观察：volatile 能停止；非 volatile 可能不能快速停止。

OrderingSummaryDemo
- 目标：验证重排序及 volatile 的有序性保障。
- 观察：普通字段可能出现 r1=0,r2=0；volatile 不应出现。
