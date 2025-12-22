# 17-threadlocal

本实验为第 17 章（ThreadLocal）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java ThreadLocalIsolationDemo
   java ThreadLocalWithInitialDemo

## 实验说明

ThreadLocalIsolationDemo
- 目标：验证 ThreadLocal 在不同线程间隔离。
- 观察：不同线程读取到自己的值，主线程读取为空。

ThreadLocalWithInitialDemo
- 目标：验证 withInitial 的默认值行为。
- 观察：未 set 时返回初始值，set 后返回新值。
