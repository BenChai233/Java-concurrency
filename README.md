# Java Concurrency

本仓库以文档 + 实验为主，示例默认面向 JDK 21。

## 章节索引

- [01.并发编程的优缺点](./01.并发编程的优缺点)
- [02.线程的状态转换以及基本操作](./02.线程的状态转换以及基本操作)
- [03.java内存模型以及happens-before规则](./03.java内存模型以及happens-before规则)
- [04.彻底理解synchronized](./04.彻底理解synchronized)
- [05.彻底理解volatile](./05.彻底理解volatile)
- [06.你以为你真的了解final吗？](./06.你以为你真的了解final吗？)
- [07.三大性质总结：原子性、可见性以及有序性](./07.三大性质总结：原子性、可见性以及有序性)
- [08.初识Lock与AbstractQueuedSynchronizer(AQS)](./08.初识Lock与AbstractQueuedSynchronizer(AQS))
- [09.深入理解AbstractQueuedSynchronizer(AQS)](./09.深入理解AbstractQueuedSynchronizer(AQS))
- [10.彻底理解ReentrantLock](./10.彻底理解ReentrantLock)
- [11.深入理解读写锁ReentrantReadWriteLock](./11.深入理解读写锁ReentrantReadWriteLock)
- [12.详解Condition的await和signal等待通知机制](./12.详解Condition的await和signal等待通知机制)
- [13.LockSupport工具](./13.LockSupport工具)
- [14.并发容器之ConcurrentHashMap(JDK 1.8版本)](./14.并发容器之ConcurrentHashMap(JDK 1.8版本))
- [15.并发容器之ConcurrentLinkedQueue](./15.并发容器之ConcurrentLinkedQueue)
- [16.并发容器之CopyOnWriteArrayList](./16.并发容器之CopyOnWriteArrayList)
- [17.并发容器之ThreadLocal](./17.并发容器之ThreadLocal)
- [18.一篇文章，从源码深入详解ThreadLocal内存泄漏问题](./18.一篇文章，从源码深入详解ThreadLocal内存泄漏问题)
- [19.并发容器之BlockingQueue](./19.并发容器之BlockingQueue)
- [20.并发容器之ArrayBlockingQueue和LinkedBlockingQueue实现原理详解](./20.并发容器之ArrayBlockingQueue和LinkedBlockingQueue实现原理详解)
- [21.线程池ThreadPoolExecutor实现原理](./21.线程池ThreadPoolExecutor实现原理)
- [22.线程池之ScheduledThreadPoolExecutor](./22.线程池之ScheduledThreadPoolExecutor)
- [23.FutureTask基本操作总结](./23.FutureTask基本操作总结)
- [24.Java中atomic包中的原子操作类总结](./24.Java中atomic包中的原子操作类总结)
- [25.大白话说java并发工具类-CountDownLatch，CyclicBarrier](./25.大白话说java并发工具类-CountDownLatch，CyclicBarrier)
- [26.大白话说java并发工具类-Semaphore，Exchanger](./26.大白话说java并发工具类-Semaphore，Exchanger)
- [27.一篇文章，让你彻底弄懂生产者--消费者问题](./27.一篇文章，让你彻底弄懂生产者--消费者问题)
- [28.Virtual Threads (JDK 21)](./28.%E8%99%9A%E6%8B%9F%E7%BA%BF%E7%A8%8B%EF%BC%88JDK%2021%EF%BC%89)
- [29.Flow API (JDK 9)](./29.Flow%20API%EF%BC%88JDK%209%EF%BC%89)
- [30.VarHandle and Memory Semantics (JDK 9)](./30.VarHandle%E4%B8%8E%E5%86%85%E5%AD%98%E8%AF%AD%E4%B9%89%EF%BC%88JDK%209%EF%BC%89)
- [31.CompletableFuture Enhancements (JDK 9+)](./31.CompletableFuture%E5%A2%9E%E5%BC%BA%EF%BC%88JDK%209%2B%EF%BC%89)
- [32.Reactor (Project Reactor)](./32.Reactor%EF%BC%88Project%20Reactor%EF%BC%89)

## 实验

- [labs/README.md](./labs/README.md)
- [labs/01-concurrency-pros-cons](./labs/01-concurrency-pros-cons) - 并发编程的优缺点
- [labs/02-thread-basics](./labs/02-thread-basics) - 线程的状态转换以及基本操作
- [labs/03-jmm-happens-before](./labs/03-jmm-happens-before) - Java 内存模型与 happens-before
- [labs/04-synchronized](./labs/04-synchronized) - 彻底理解 synchronized
- [labs/05-volatile](./labs/05-volatile) - 彻底理解 volatile
- [labs/06-final](./labs/06-final) - 你以为你真的了解 final 吗
- [labs/07-atomicity-visibility-ordering](./labs/07-atomicity-visibility-ordering) - 原子性/可见性/有序性总结
- [labs/08-lock-aqs-intro](./labs/08-lock-aqs-intro) - 初识 Lock 与 AQS
- [labs/09-aqs-deep-dive](./labs/09-aqs-deep-dive) - 深入理解 AQS
- [labs/10-reentrantlock](./labs/10-reentrantlock) - 彻底理解 ReentrantLock
- [labs/11-reentrantreadwritelock](./labs/11-reentrantreadwritelock) - 深入理解读写锁 ReentrantReadWriteLock
- [labs/12-condition-await-signal](./labs/12-condition-await-signal) - 详解 Condition 的 await 和 signal
- [labs/13-locksupport](./labs/13-locksupport) - LockSupport 工具
- [labs/14-concurrenthashmap](./labs/14-concurrenthashmap) - 并发容器之 ConcurrentHashMap
- [labs/15-concurrentlinkedqueue](./labs/15-concurrentlinkedqueue) - 并发容器之 ConcurrentLinkedQueue
- [labs/16-copyonwritearraylist](./labs/16-copyonwritearraylist) - 并发容器之 CopyOnWriteArrayList
- [labs/17-threadlocal](./labs/17-threadlocal) - 并发容器之 ThreadLocal
- [labs/18-threadlocal-leak](./labs/18-threadlocal-leak) - ThreadLocal 内存泄漏
- [labs/19-blockingqueue](./labs/19-blockingqueue) - 并发容器之 BlockingQueue
- [labs/20-arrayblockingqueue-linkedblockingqueue](./labs/20-arrayblockingqueue-linkedblockingqueue) - ArrayBlockingQueue 与 LinkedBlockingQueue
- [labs/21-threadpoolexecutor](./labs/21-threadpoolexecutor) - 线程池 ThreadPoolExecutor
- [labs/22-scheduledthreadpoolexecutor](./labs/22-scheduledthreadpoolexecutor) - ScheduledThreadPoolExecutor
- [labs/23-futuretask](./labs/23-futuretask) - FutureTask
- [labs/24-atomic](./labs/24-atomic) - atomic 包原子类
- [labs/25-countdownlatch-cyclicbarrier](./labs/25-countdownlatch-cyclicbarrier) - CountDownLatch 与 CyclicBarrier
- [labs/26-semaphore-exchanger](./labs/26-semaphore-exchanger) - Semaphore 与 Exchanger
- [labs/27-producer-consumer](./labs/27-producer-consumer) - 生产者-消费者
- [labs/28-virtual-threads](./labs/28-virtual-threads) - Virtual Threads
- [labs/29-flow](./labs/29-flow) - Flow API
- [labs/30-varhandle](./labs/30-varhandle) - VarHandle and memory semantics
- [labs/31-completablefuture](./labs/31-completablefuture) - CompletableFuture enhancements
- [labs/32-reactor-foundation](./labs/32-reactor-foundation) - Reactor foundations via JDK Flow

## 计划

- [plan.md](./plan.md)

## 运行说明

- 确认版本：`java -version`
- 示例：`cd labs/01-concurrency-pros-cons; javac *.java; java ConcurrencyBenefits`
