# 11-reentrantreadwritelock

本实验为第 11 章（ReentrantReadWriteLock）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java ReadWriteLockReadConcurrencyDemo [readers] [holdMs]
   java ReadWriteLockDowngradeDemo
   java ReadWriteLockStarvationDemo [readers] [holdMs] [maxWaitMs]

## 实验说明

ReadWriteLockReadConcurrencyDemo
- 目标：验证读锁的并发性与写锁的互斥性。
- 观察：读锁总耗时接近 holdMs；写锁总耗时接近 readers * holdMs。

ReadWriteLockDowngradeDemo
- 目标：演示写锁降级为读锁，以及读锁升级的限制。
- 观察：降级成功；在持有读锁时升级写锁会失败（超时返回 false）。

ReadWriteLockStarvationDemo
- 目标：观察非公平读锁下写线程等待时间可能变长。
- 观察：非公平模式下 writerWaitMs 可能更大或超时；公平模式更容易尽快获得写锁。
