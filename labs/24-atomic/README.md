# 24-atomic

本实验为第 24 章（atomic 包）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java AtomicIntegerIncrementDemo [threads] [iterations]

## 实验说明

AtomicIntegerIncrementDemo
- 目标：对比非原子自增与 AtomicInteger 的正确性。
- 观察：nonAtomic 往往小于 expected，atomic 等于 expected。
