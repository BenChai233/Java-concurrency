# 27-producer-consumer

本实验为第 27 章（生产者-消费者）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java ProducerConsumerWaitNotifyDemo [capacity] [producers] [consumers] [itemsPerProducer]

## 实验说明

ProducerConsumerWaitNotifyDemo
- 目标：用 wait/notify 实现有界缓冲区，演示生产者-消费者协作。
- 观察：生产者在缓冲区满时等待，消费者在空时等待；所有线程完成后输出统计信息。
