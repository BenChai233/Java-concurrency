# 14-concurrenthashmap

本实验为第 14 章(ConcurrentHashMap)对应实验。

## 如何运行

在本目录执行:

1) 编译:
   javac *.java

2) 运行:
   java ConcurrentHashMapPutIfAbsentDemo [threads]
   java ConcurrentHashMapWeaklyConsistentIterationDemo [initial] [adds]

## 实验说明

ConcurrentHashMapPutIfAbsentDemo
- 目标:验证 putIfAbsent 的只有一个线程成功写入。
- 观察:winnerCount 约等于 1，最终 mapSize=1。

ConcurrentHashMapWeaklyConsistentIterationDemo
- 目标:验证并发写入时迭代不抛异常(弱一致性)。
- 观察:iteratedCount 位于 [initial, initial+adds]，且无 ConcurrentModificationException。

