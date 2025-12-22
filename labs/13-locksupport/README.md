# 13-locksupport

本实验为第 13 章（LockSupport）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java LockSupportPermitDemo
   java LockSupportInterruptDemo

## 实验说明

LockSupportPermitDemo
- 目标：验证 unpark 先于 park 的“许可”语义，以及许可不累计。
- 观察：第一次 park 立即返回；第二次 park 会阻塞直到再次 unpark。

LockSupportInterruptDemo
- 目标：验证 interrupt 会使 park 立刻返回，并保留中断标记。
- 观察：被中断后 park 返回，`isInterrupted()` 为 true。
