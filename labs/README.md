# Labs Index

实验目录按章节编号组织，每个实验尽量只验证一个结论。

## 目录

- [01-concurrency-pros-cons](./01-concurrency-pros-cons) - 并发编程的优缺点
- [02-thread-basics](./02-thread-basics) - 线程的状态转换以及基本操作
- [03-jmm-happens-before](./03-jmm-happens-before) - Java 内存模型与 happens-before
- [04-synchronized](./04-synchronized) - 彻底理解 synchronized
- [05-volatile](./05-volatile) - 彻底理解 volatile
- [06-final](./06-final) - 你以为你真的了解 final 吗
- [07-atomicity-visibility-ordering](./07-atomicity-visibility-ordering) - 原子性/可见性/有序性总结
- [08-lock-aqs-intro](./08-lock-aqs-intro) - 初识 Lock 与 AQS
- [09-aqs-deep-dive](./09-aqs-deep-dive) - 深入理解 AQS
- [10-reentrantlock](./10-reentrantlock) - 彻底理解 ReentrantLock
- [11-reentrantreadwritelock](./11-reentrantreadwritelock) - 深入理解读写锁 ReentrantReadWriteLock
- [12-condition-await-signal](./12-condition-await-signal) - 详解 Condition 的 await 和 signal
- [13-locksupport](./13-locksupport) - LockSupport 工具
- [14-concurrenthashmap](./14-concurrenthashmap) - 并发容器之 ConcurrentHashMap
- [15-concurrentlinkedqueue](./15-concurrentlinkedqueue) - 并发容器之 ConcurrentLinkedQueue
- [16-copyonwritearraylist](./16-copyonwritearraylist) - 并发容器之 CopyOnWriteArrayList
- [17-threadlocal](./17-threadlocal) - 并发容器之 ThreadLocal
- [18-threadlocal-leak](./18-threadlocal-leak) - ThreadLocal 内存泄漏
- [19-blockingqueue](./19-blockingqueue) - 并发容器之 BlockingQueue
- [20-arrayblockingqueue-linkedblockingqueue](./20-arrayblockingqueue-linkedblockingqueue) - ArrayBlockingQueue 与 LinkedBlockingQueue
- [21-threadpoolexecutor](./21-threadpoolexecutor) - 线程池 ThreadPoolExecutor
- [22-scheduledthreadpoolexecutor](./22-scheduledthreadpoolexecutor) - ScheduledThreadPoolExecutor
- [23-futuretask](./23-futuretask) - FutureTask
- [24-atomic](./24-atomic) - atomic 包原子类
- [25-countdownlatch-cyclicbarrier](./25-countdownlatch-cyclicbarrier) - CountDownLatch 与 CyclicBarrier
- [26-semaphore-exchanger](./26-semaphore-exchanger) - Semaphore 与 Exchanger
- [27-producer-consumer](./27-producer-consumer) - 生产者-消费者

## 统一运行方式

- 编译：`javac *.java`
- 运行：`java <MainClass>`

每个实验目录内的 README 会给出参数与观察点。
