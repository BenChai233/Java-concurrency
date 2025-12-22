# 20-arrayblockingqueue-linkedblockingqueue

本实验为第 20 章（ArrayBlockingQueue / LinkedBlockingQueue）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java BlockingQueueThroughputCompareDemo [totalOps] [producers] [consumers] [capacity]

## 实验说明

BlockingQueueThroughputCompareDemo
- 目标：对比 ArrayBlockingQueue 与 LinkedBlockingQueue 在相同负载下的吞吐表现。
- 观察：两者 timeMs 与 opsPerSec 可能不同；在你的机器上记录差异与波动范围。
