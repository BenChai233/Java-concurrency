# 25-countdownlatch-cyclicbarrier

本实验为第 25 章（CountDownLatch / CyclicBarrier）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java CountDownLatchDemo [workers]
   java CyclicBarrierDemo [parties]

## 实验说明

CountDownLatchDemo
- 目标：验证 CountDownLatch 只能等待一次，用于主线程等待子任务完成。
- 观察：主线程阻塞到所有 worker 完成后继续。

CyclicBarrierDemo
- 目标：验证 CyclicBarrier 可复用，用于多阶段同步。
- 观察：barrier 触发两次，所有 worker 先完成 phase-1 再进入 phase-2。
