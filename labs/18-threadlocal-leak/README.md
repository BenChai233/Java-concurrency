# 18-threadlocal-leak

本实验为第 18 章（ThreadLocal 内存泄漏）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行（建议小堆以便观察）：
   java -Xms64m -Xmx64m ThreadLocalLeakDemo [iterations] [mbPerValue] [remove]

## 实验说明

ThreadLocalLeakDemo
- 目标：演示线程池线程中 ThreadLocal 未 remove 时的内存滞留风险。
- 观察：remove=false 时 usedMB 可能持续上升；remove=true 更稳定。
- 说明：效果与 GC/堆大小有关，可适当增大 iterations 以强化现象。
