# 08-lock-aqs-intro

本实验为第 08 章（初识 Lock 与 AQS）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java LockBasicsDemo [threads] [loops]
   java TryLockTimeoutDemo [holdMs] [timeoutMs]
   java LockInterruptDemo
   java AqsSharedExclusiveDemo

## 实验说明

LockBasicsDemo
- 目标：ReentrantLock 的基本用法与 finally 解锁。
- 观察：加锁后计数正确。

TryLockTimeoutDemo
- 目标：验证超时 tryLock 的行为。
- 观察：在锁被占用时返回 false。

LockInterruptDemo
- 目标：验证 lockInterruptibly 可响应中断。
- 观察：等待线程被中断后退出。

AqsSharedExclusiveDemo
- 目标：对比共享与独占同步器的行为。
- 观察：Semaphore 允许并发通过；CountDownLatch 一次性释放。
