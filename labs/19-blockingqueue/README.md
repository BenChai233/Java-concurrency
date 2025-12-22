# 19-blockingqueue

本实验为第 19 章（BlockingQueue）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java BlockingQueueBlockingDemo

## 实验说明

BlockingQueueBlockingDemo
- 目标：验证 take/put 的阻塞语义。
- 观察：Phase 1 中 consumer 等待一段时间后才拿到元素；Phase 2 中 producer 在队列满时等待，直到 consumer 取走元素。
