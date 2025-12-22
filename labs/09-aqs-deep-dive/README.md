# 09-aqs-deep-dive

本实验为第 09 章（深入理解 AQS）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java FairVsNonfairLockDemo [threads]
   java SimpleAqsMutexDemo [threads] [loops]
   java AqsConditionQueueDemo

## 实验说明

FairVsNonfairLockDemo
- 目标：对比公平锁与非公平锁的获取顺序。
- 观察：公平锁更接近 FIFO，非公平锁吞吐更高。

SimpleAqsMutexDemo
- 目标：基于 AQS 实现简单互斥锁。
- 观察：计数正确，互斥生效。

AqsConditionQueueDemo
- 目标：验证 await/signal 的正确用法。
- 观察：未持锁调用会抛 IllegalMonitorStateException；signal 唤醒等待线程。
