# 05-volatile

本实验为第 05 章（volatile）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java VolatileStopFlagDemo
   java VolatileAtomicityDemo [threads] [loops]
   java VolatileReorderingDemo [iterations]
   java VolatilePublishDemo [iterations]

## 实验说明

VolatileStopFlagDemo
- 目标：验证 volatile 标志位的可见性。
- 观察：volatile 能及时停止；非 volatile 可能不能快速停止。

VolatileAtomicityDemo
- 目标：验证 volatile 不提供原子性。
- 观察：多线程自增仍会小于期望值。

VolatileReorderingDemo
- 目标：验证普通字段可能发生重排序。
- 观察：普通字段可能出现 r1=0,r2=0；volatile 应该不会出现。

VolatilePublishDemo
- 目标：验证 volatile 引用的安全发布。
- 观察：volatile 模式错误为 0；普通模式可能出现错误（概率性）。
