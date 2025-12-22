# 10-reentrantlock

本实验为第 10 章（ReentrantLock）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java ReentrantLockReentrancyDemo
   java ReentrantLockFairnessPerfDemo [threads] [loops]
   java ReentrantLockConditionDemo
   java ReentrantLockInterruptDemo

## 实验说明

ReentrantLockReentrancyDemo
- 目标：验证可重入性。
- 观察：递归调用正常完成。

ReentrantLockFairnessPerfDemo
- 目标：对比公平/非公平锁性能。
- 观察：非公平通常更快。

ReentrantLockConditionDemo
- 目标：验证多个 Condition 的精确唤醒。
- 观察：不同条件队列互不干扰。

ReentrantLockInterruptDemo
- 目标：验证 lockInterruptibly 可响应中断。
- 观察：等待线程被中断后退出。
