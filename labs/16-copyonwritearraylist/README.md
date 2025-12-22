# 16-copyonwritearraylist

本实验为第 16 章（CopyOnWriteArrayList）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java CopyOnWriteArrayListSnapshotDemo
   java CopyOnWriteArrayListIterationDemo

## 实验说明

CopyOnWriteArrayListSnapshotDemo
- 目标：验证迭代器是快照视图。
- 观察：迭代结果不包含后续 add 的元素。

CopyOnWriteArrayListIterationDemo
- 目标：验证并发修改时迭代不抛异常。
- 观察：主线程迭代稳定完成，且不受修改线程影响。
