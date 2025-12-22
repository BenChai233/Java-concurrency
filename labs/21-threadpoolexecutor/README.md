# 21-threadpoolexecutor

本实验为第 21 章（ThreadPoolExecutor）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java ThreadPoolExecutorSizingDemo

## 实验说明

ThreadPoolExecutorSizingDemo
- 目标：观察 corePoolSize、maximumPoolSize、队列容量与拒绝策略的协同效果。
- 观察：第 4 个任务被拒绝；任务执行时线程数从 1 扩到 2；空闲后非核心线程回收。
