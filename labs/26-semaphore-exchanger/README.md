# 26-semaphore-exchanger

本实验为第 26 章（Semaphore / Exchanger）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java SemaphoreDemo [permits] [workers]
   java ExchangerDemo

## 实验说明

SemaphoreDemo
- 目标：验证 Semaphore 控制并发进入数量。
- 观察：maxObserved 不会超过 permits。

ExchangerDemo
- 目标：验证 Exchanger 在两个线程之间交换数据。
- 观察：A/B 输出的 after 数据互换。
