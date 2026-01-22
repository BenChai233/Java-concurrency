# 01.并发编程的优缺点
1. 并发带来净收益的三个前提是什么？
2. 为什么“线程越多越快”通常是错的？
3. CPU 密集任务的并发度经验值应如何选？
4. I/O 等待密集任务为什么更适合虚拟线程？
5. 什么情况下虚拟线程不一定有效？
6. 并发安全主要要解决哪三类问题？
7. 死锁出现的典型条件（简述即可）？
8. 锁竞争除了让吞吐下降，还会带来什么更隐蔽的问题？
9. 缓存一致性开销在代码层面常见诱因是什么？
10. 你要写一段并发代码，最先做的两件事是什么？

# 02.线程的状态转换以及基本操作
1. Java 中新建线程的三种方式分别是什么？各自的典型用法是什么？
2. 为什么在创建线程时更推荐“实现接口”的方式？
3. Callable 提交到 ExecutorService 后得到的是什么？与 Runnable 的差异点是什么？
4. FutureTask 与 Callable / Runnable 的关系各是什么？
5. 线程从 NEW 到 RUNNABLE、WAITING、TIMED_WAITING、BLOCKED、TERMINATED 的触发条件分别是什么？
6. wait/join/LockSupport.park 与 sleep 的状态差异是什么？
7. synchronized 竞争锁与 Lock 加锁时，线程可能进入哪些状态？为什么不同？
8. interrupt()、isInterrupted()、Thread.interrupted() 的语义与差异是什么？
9. join 的实现为何是 while (isAlive()) { wait(0); }？它意味着什么？
10. 守护线程的退出时机与 finally 执行有什么风险？

# 03.java内存模型以及happens-before规则
1. JMM 里“可见性问题”指的是什么？为什么会发生“写了但另一个线程读不到”的现象？
2. happens-before 是“谁对谁”的保证？它保证了什么、不保证什么？
3. volatile 写-读建立的 happens-before 具体是什么规则？它解决了哪两类问题（可见性/有序性）中的哪些？
4. 在 Publication 模式中，为什么正确顺序必须是 data = ... 然后 ready = true(volatile)？如果反过来会出现什么现象？
5. “看到 ready=true 但读到旧 data”在 JMM 语义下属于什么问题：可见性、重排序，还是逻辑协议写错？
6. useVolatile=false 的实验有时跑不出问题，能否据此说明“普通 boolean 也没事”？为什么？
7. Thread.start() 与 Thread.join() 分别建立了哪些 happens-before 关系？它们能否替代 volatile？
8. STOP_FLAG 模式里，读线程为什么可能一直自旋看不到 stop=true？从“编译器/JIT 优化”角度说可能发生了什么？
9. 为什么在并发可见性实验里，加入 System.nanoTime()、sleep/park 这类调用可能让问题更难复现？
10. 如果你要在业务代码里实现“发布-订阅（data + ready）”，你会如何写出读/写两侧的最小正确模板？

# 04.彻底理解synchronized
1. synchronized 解决的三个核心问题分别是什么（原子性/可见性/有序性），各自是如何被保证的？
2. value++ 在字节码/JMM 视角下分解成哪些步骤？为什么在并发下会“丢增量”？
3. synchronized 修饰实例方法、静态方法、代码块时，锁对象分别是谁？
4. 同一段代码里出现两个 synchronized，如何判断它们是否“用的是同一把锁”？
5. 为什么“写在 synchronized 里”不等于“其他线程随便读就一定可见”？需要什么 happens-before 才能保证？
6. monitorenter/monitorexit 的内存语义分别对应 acquire/release 的哪一侧？对重排序有什么约束？
7. CountDownLatch.await() / Thread.join() 为什么能保证主线程读到工作线程的最终写入？它们建立了什么 happens-before？
8. 把 get() 也加上 synchronized 后，和仅靠 done.await() 保证可见性相比，有什么差异与代价？
9. 什么时候应该用 synchronized，什么时候更适合用 AtomicInteger / LongAdder / ReentrantLock？
10. 如何避免“锁住太大范围”导致的性能问题？你会用哪些手段衡量（吞吐/锁竞争/线程状态）？

# 05.彻底理解volatile
1. volatile 解决并发的哪两类问题（可见性、禁止部分重排序），不解决哪一类（复合操作原子性）？
2. 写一个例子说明：count++ 即使 count 是 volatile，为什么仍然会丢失更新？
3. “对同一个 volatile 变量的写”和“随后对它的读”之间建立了什么关系（happens-before）？带来什么保证？
4. volatile 写/读分别相当于插入了哪类内存屏障效果（store-store / store-load / load-load / load-store 的直觉即可）？
5. 为什么 volatile 常用于“停止标志” (volatile boolean stop)？如果不用 volatile 可能出现什么现象？
6. “安全发布”有哪些常见手段？分别举例：volatile 发布、final 字段语义、锁、线程安全容器、静态初始化。
7. volatile 能保证引用本身可见，那它能否保证“对象内部状态永远一致”？什么情况下仍可能读到不一致（竞态更新）？
8. 双重检查锁（DCL）里为什么 instance 必须是 volatile？不加会有什么风险（看到“半初始化对象”）？
9. 在自旋等待里用 Thread.onSpinWait() 的目的是什么？它会改变内存可见性语义吗？
10. 什么时候应该用 AtomicInteger/LongAdder 或 synchronized/Lock，而不是用 volatile 硬扛？

# 06.java关键字--final
1. final 能修饰哪些位置（变量/方法/类）？分别带来什么“限制”？
2. final static 类变量有哪些“允许初始化的位置”？为什么只能在这些位置初始化一次？
3. final 实例变量有哪些“允许初始化的位置”？如果没初始化，编译器会怎么处理？
4. final 局部变量的赋值规则是什么？“只赋值一次”在分支/循环里如何判定？
5. final 修饰基本类型与修饰引用类型的本质区别是什么？哪些能变、哪些不能变？
6. 何谓“宏变量/编译期常量”？满足哪 3 个条件时，编译器会做什么替换行为？
7. final 方法为什么不能被重写？举一个 JDK 里常见的 final 方法例子并说明影响。
8. final 类为什么不能被继承？举一个典型 final 类例子，并说明这样设计的收益/代价。
9. JMM 对 final 域的写与读各有哪些重排序禁止规则？分别要防住什么并发可见性问题？
10. 为什么“final 引用不能从构造函数中逸出（this escape）”？结合“构造内赋值 + 把 this 赋给共享引用”的例子，说明可能出现的错误现象。

# 07.三大性质总结：原子性、可见性以及有序性
1. 用一句话分别解释：什么是原子性、可见性、有序性？各自解决什么“线程安全”问题？
2. int a = 10; a++; int b=a; a=a+1; 哪些是原子操作？为什么？
3. a++ 在字节码/执行层面可以拆成哪三个步骤？这三个步骤为什么导致并发下结果丢失？
4. JMM 定义的 8 种原子操作分别是什么（lock/unlock/read/load/use/assign/store/write）？“从主内存到工作内存”和“从工作内存回主内存”分别对应哪两组操作？
5. 为什么说“JMM 只要求 read->load、store->write 顺序执行，但不要求连续执行”？这会带来什么直观后果？
6. 为什么 synchronized 能保证更大范围的原子性？它对应了哪些 JVM 层面的指令语义（monitorenter/monitorexit）？
7. 解释：为什么 volatile int counter 仍然不能让 counter++ 线程安全？你能描述一个导致最终结果小于期望值的并发交错过程吗？
8. 书中给出的“volatile 能保证原子性”的两个前提条件是什么？分别举一个“满足/不满足”的例子。
9. 解释“有序性”的核心总结：为什么“线程内看有序、跨线程看无序”？它与编译器/CPU 指令重排序有什么关系？
10. 双重检查锁（DCL）单例里为什么 instance 必须加 volatile？如果不加，new Singleton() 的哪三个子步骤可能重排序，进而造成什么问题？

# 08.初识Lock与AbstractQueuedSynchronizer(AQS)
1. java.util.concurrent 包大致分哪些子模块？atomic、locks、阻塞队列、executors 各自解决什么问题？
2. 相比 synchronized，Lock 主要多了哪些能力？
3. 为什么 Lock 必须在 finally 里 unlock()？如果忘记释放锁，最常见的后果是什么？
4. lock()、lockInterruptibly()、tryLock()、tryLock(timeout) 的“阻塞/可中断/返回值语义”分别是什么？
5. Condition 和 Object.wait/notify 的对应关系是什么？它们对“必须先持有锁/监视器”的要求有什么相同与不同？
6. AQS 解决的核心问题是什么？（提示：同步状态管理 + 线程排队/阻塞唤醒的通用框架）
7. AQS 里的 state 通常代表什么？它一定是“0/1 二值”吗？什么场景会用“计数语义”？
8. 用 AQS 自定义一个独占锁时，一般需要重写哪些 protected 方法？它们分别在“获取/释放”时承担什么职责？
9. AQS 的“独占模式”和“共享模式”有什么区别？各自对应要重写哪组 tryAcquire* / tryRelease* 方法？
10. 为什么说 AQS 采用了“模板方法”设计？同步组件作者需要关注什么，AQS 又帮你屏蔽了什么？

# 09.深入理解AbstractQueuedSynchronizer(AQS)
1. AQS 在同步组件里扮演什么角色？它“提供的”和“组件实现者要实现的”分别是什么？
2. AQS 的同步队列是什么数据结构（单/双向、是否有 head/tail）？为什么要用 head/tail 指针？
3. Node 里关键字段有哪些（waitStatus/prev/next/thread/nextWaiter），各自解决什么问题？
4. waitStatus 的 CANCELLED/SIGNAL/CONDITION/PROPAGATE/0 分别表示什么含义？哪些只会出现在条件队列？
5. 独占获取 acquire() 的主流程是什么？tryAcquire 失败后会经历哪些关键步骤（入队/自旋/挂起/被唤醒）？
6. 自旋获取锁时，“什么时候有资格再 tryAcquire 一次并可能成功退出循环”？为什么要检查“前驱是否为 head”？
7. shouldParkAfterFailedAcquire(p,node) 的核心意图是什么？为什么不能一入队就直接 park？
8. 释放独占锁 release() 的关键点是什么？为什么只唤醒（通常）head 的后继节点？遇到 CANCELLED 节点怎么处理？
9. 可中断获取 acquireInterruptibly() 与不可中断的核心差异是什么？中断发生在“等待中”会怎样收敛成可见结果？
10. 共享获取/释放与独占相比，acquireShared()/releaseShared() 在“成功条件”和“唤醒传播”上最大的不同点是什么？PROPAGATE 的意义是什么？

# 10.彻底理解ReentrantLock
1. ReentrantLock 为什么叫“可重入锁”？可重入要解决哪两个核心问题？
2. nonfairTryAcquire 里 state == 0 与 current == getExclusiveOwnerThread() 两个分支分别代表什么场景？
3. 同一线程连续 lock() 3 次，AQS 的 state 变化过程是什么？对应需要 unlock() 几次才算“完全释放”？
4. tryRelease 中为什么要校验 Thread.currentThread() != getExclusiveOwnerThread()？不校验会有什么风险？
5. 公平锁的“公平”具体公平在哪里？它保证的是哪种意义上的 FIFO？
6. 公平锁 tryAcquire 里 hasQueuedPredecessors() 的作用是什么？它会改变哪一步的获取锁决策？
7. 非公平锁相比公平锁的主要收益是什么？主要代价是什么（从上下文切换/吞吐/饥饿角度回答）？
8. 为什么 ReentrantLock 默认构造是“非公平锁”？这反映了什么工程取舍？
9. 非公平锁为什么更容易发生“饥饿”？举一个可能出现饥饿的线程竞争场景。
10. 从“重入计数”的角度，对比 synchronized 与 ReentrantLock 的重入机制相同点与不同点各是什么？

# 11.深入理解读写锁ReentrantReadWriteLock
1. 什么业务场景更适合用 ReentrantReadWriteLock，它相对 synchronized/ReentrantLock 解决了什么性能瓶颈？
2. 读锁/写锁的互斥关系分别是什么？
3. ReentrantReadWriteLock 的“公平/非公平”模式差别是什么，为什么默认非公平吞吐更高？
4. 同步状态 state 是如何同时表示“读次数”和“写次数”的？
5. 写锁 tryAcquire 的核心判定流程是什么？哪些情况下会直接失败？
6. 写锁释放 tryRelease 做了哪些事，什么时候真正“完全释放”？
7. 读锁 tryAcquireShared 为什么要先判断“是否有其他线程持有写锁”？
8. 读锁获取成功后，为什么还要维护 firstReader/firstReaderHoldCount、以及HoldCounter(ThreadLocal)？
9. 读锁释放 tryReleaseShared 的核心逻辑是什么，为什么要用循环 CAS 扣减 state？
10. 什么是“锁降级”，标准步骤是什么？为什么“不支持锁升级”？

# 12.详解Condition的await和signal等待通知机制
1. Condition 相比 Object.wait/notify 的 3 个核心差异是什么？
2. await() 调用时线程做了哪几件事？（入条件队列、释放锁、LockSupport.park 挂起）
3. 为什么说 Condition 的等待队列是“单向队列”？对应的字段是什么？（Node.nextWaiter；firstWaiter/lastWaiter）
4. AQS 的“同步队列”和 Condition 的“等待队列”各自存什么线程？线程在两者之间如何流转？
5. signal() 做的关键动作是什么？（把等待队列头结点转移到同步队列：transferForSignal + enq）
6. 为什么 signal() 的前提是“当前线程必须已经持有 lock”？不满足会怎样？（语义不成立/直接抛 IllegalMonitorStateException 风格问题）
7. 为什么被 signal 的线程不会立刻从 await() 返回？它还必须满足什么条件？（在同步队列里重新竞争并获取到 lock）
8. signal() 与 signalAll() 的本质区别是什么？对应源码层面的差异点在哪里？（doSignal 只处理头结点；doSignalAll 遍历搬运全部）
9. 例子里为什么用 while (!flag) condition.await() 而不是 if？
10. 结合“开关模型”：等待方与通知方的正确协作顺序是什么？（等待方先 lock+await；通知方 lock+改条件+signal/signalAll+unlock）

# 13.LockSupport工具
1. LockSupport 位于哪个包、主要用途是什么？
2. LockSupport 的“许可”语义是什么？
3. park() 会在什么情况下返回？
4. unpark(Thread) 的作用是什么？
5. parkNanos 与 parkUntil 的差异？
6. 为何 park(Object blocker) 需要传入 blocker？
7. 线程 dump 中 park() 和 park(Object) 的差别体现在哪里？
8. LockSupport 的阻塞/唤醒依赖哪个底层类实现？
9. synchronized 阻塞与 LockSupport 阻塞时线程状态有何不同？
10. 示例中 thread 被唤醒的流程是什么？

# 14.并发容器之ConcurrentHashMap(JDK 1.8版本)
1. 对比 HashMap / Hashtable / Collections.synchronizedMap / ConcurrentHashMap(JDK 8)：它们各自的线程安全手段与锁粒度差异是什么？
2. ConcurrentHashMap 的 table 数组里可能出现哪些节点形态（如 Node / TreeBin / ForwardingNode），它们分别代表什么语义？
3. sizeCtl 在为 -1、-N、0、正数时分别表示什么状态？这些状态分别在初始化与扩容流程的哪些节点被读/写？
4. putVal 主流程中，哪些步骤走 CAS（例如 casTabAt），哪些步骤走 synchronized？各自保护的共享数据/临界区是什么？
5. 扩容 transfer 过程中，transferIndex、stride、ForwardingNode(MOVED) 分别承担什么协作角色？多个线程如何避免重复迁移同一个桶？
6. 你要在并发插入/删除同时发生时“统计当前 map 大小”用于监控与告警：你会选 size() 还是 mappingCount()？为什么？
7. 线上出现“偶发长尾延迟”，你怀疑来自触发扩容：结合 addCount() 的触发条件与扩容协作机制，你会如何设计一个最小复现实验来验证？
8. 代码阅读：阅读 `spread` 方法：表达式 `(h ^ (h >>> 16)) & HASH_BITS` 想解决什么问题？当 n=16 时，索引 `i=(n-1)&spread(h)` 的取值范围是什么？

    ```java
    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }
    ```

9. 代码阅读：阅读 `tableSizeFor` 方法：它返回的是什么？对 c=18、c=16、c=1、c=0 分别会返回什么？

    ```java
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 :
               (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
    ```

10. 代码阅读：迁移时为何按 `(ph & n)` 分成两部分并分别落在 `nextTable` 的 i 与 i+n？最后 `setTabAt(tab,i,fwd)` 的语义是什么？

    ```java
    if ((ph & n) == 0)
        ln = new Node<K,V>(ph, pk, pv, ln);
    else
        hn = new Node<K,V>(ph, pk, pv, hn);

    setTabAt(nextTab, i, ln);
    setTabAt(nextTab, i + n, hn);
    setTabAt(tab, i, fwd);
    ```

