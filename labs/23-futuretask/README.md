# 23-futuretask

本实验为第 23 章（FutureTask）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java FutureTaskSingleExecutionDemo

## 实验说明

FutureTaskSingleExecutionDemo
- 目标：验证 FutureTask 的任务只会执行一次。
- 观察：runCount=1，两个 runner 调用 run 但只有一次真正执行。
