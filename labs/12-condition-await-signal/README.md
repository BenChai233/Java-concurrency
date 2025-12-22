# 12-condition-await-signal

本实验为第 12 章（Condition 的 await/signal）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java ConditionWhileGuardDemo
   java ConditionInterruptDemo

## 实验说明

ConditionWhileGuardDemo
- 目标：验证 await 必须配合 while 重新检查条件。
- 观察：if 版本在 signalAll 后可能出现计数为负；while 版本保持正确。

ConditionInterruptDemo
- 目标：验证 await 可被中断，并观察中断标记变化。
- 观察：await 抛出 InterruptedException，当前线程的中断标记被清除。
