# 04-synchronized

本实验为第 04 章（synchronized）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java SynchronizedAtomicityDemo [threads] [loops]
   java SynchronizedVisibilityDemo
   java SynchronizedReentrancyDemo [depth]
   java SynchronizedLockScopeDemo [sleepMs]

## 实验说明

SynchronizedAtomicityDemo
- 目标：验证 synchronized 对复合操作的原子性保障。
- 观察：无锁计数通常小于期望值；加锁后等于期望值。

SynchronizedVisibilityDemo
- 目标：验证同一监视器下的可见性（happens-before）。
- 观察：读线程能看到写线程的最新值。

SynchronizedReentrancyDemo
- 目标：验证可重入性。
- 观察：嵌套调用正常完成，无死锁。

SynchronizedLockScopeDemo
- 目标：区分对象锁与类锁。
- 观察：同一实例互斥；不同实例可并发；类锁与对象锁互不阻塞。
