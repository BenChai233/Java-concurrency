# 22-scheduledthreadpoolexecutor

本实验为第 22 章（ScheduledThreadPoolExecutor）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java ScheduledThreadPoolExecutorFixedRateVsDelayDemo

## 实验说明

ScheduledThreadPoolExecutorFixedRateVsDelayDemo
- 目标：对比 scheduleAtFixedRate 与 scheduleWithFixedDelay 在任务耗时超过周期时的行为差异。
- 观察：fixed-rate 的 intervalMs 贴近任务耗时；fixed-delay 的 intervalMs 约为 任务耗时 + delay。
