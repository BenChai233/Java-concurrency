# 15-concurrentlinkedqueue

本实验为第 15 章(ConcurrentLinkedQueue)对应实验。

## 如何运行

在本目录执行:

1) 编译:
   javac *.java

2) 运行:
   java ConcurrentLinkedQueueFifoDemo
   java ConcurrentLinkedQueueConcurrentDemo [producers] [perProducer]

## 实验说明

ConcurrentLinkedQueueFifoDemo
- 目标:验证 FIFO 顺序。
- 观察:poll 出来的顺序与 offer 顺序一致。

ConcurrentLinkedQueueConcurrentDemo
- 目标:验证多生产者/消费者下的并发安全。
- 观察:consumed 总数等于 expected，总耗时可用于复盘。

