# 第 05 章实验：volatile

用 4 个最小实验建立对 `volatile` 的直觉：可见性、有序性、（不提供）原子性、以及安全发布。

## 编译

```powershell
java -version
cd labs\05-volatile
javac *.java
```

## 实验 A：停止标志的可见性（`VolatileStopFlagDemo`）

```powershell
java VolatileStopFlagDemo
```

观察点：
- `volatile` 标志位通常能让工作线程及时退出；非 `volatile` 版本可能长时间不退出（或需要额外干预/等待）。
- 若机器/负载不同导致现象不明显，可多运行几次对比（这是概率性现象）。

## 实验 B：volatile 不保证原子性（`VolatileAtomicityDemo`）

```powershell
java VolatileAtomicityDemo [threads] [loops]
# 例如：
java VolatileAtomicityDemo 8 1000000
```

观察点：
- 即使计数变量是 `volatile`，多线程 `++` 的最终结果仍可能小于期望值（丢失更新）。
- 增大 `threads/loops` 更容易复现偏差。

## 实验 C：缺少 happens-before 的“反直觉”结果（`VolatileReorderingDemo`）

```powershell
java VolatileReorderingDemo [iterations]
# 例如：
java VolatileReorderingDemo 200000
```

观察点：
- `Plain`（普通字段）可能观察到 `r1=0,r2=0`（经典 store-buffering：两边都“先写后读”但读到旧值）。
- `WithVolatile`（`x/y` 都是 `volatile`）按 JMM 语义不应出现 `r1=0,r2=0`；若未观察到对比，尝试提高 `iterations`。

## 实验 D：volatile 引用的安全发布（`VolatilePublishDemo`）

```powershell
java VolatilePublishDemo [iterations]
# 例如：
java VolatilePublishDemo 200000
```

观察点：
- `volatile` 发布路径下，不应观察到“看到对象引用但对象内部状态未初始化完成”的异常现象。
- 普通发布路径下，异常现象通常是概率性出现；增大 `iterations` 更容易复现。
- x86/amd64 很“强内存序”：它基本不允许把“先写字段、再写引用”重排成“先写引用、再写字段”（store-store 重排），所以“读到非 null 但字段还是 0”的情况极难出现。
- HotSpot/JIT 通常也不爱把这种初始化发布重排到出事：JMM 允许，但实现上不一定会真的这么做（尤其在这个简单模式里）。
- 这是数据竞争 bug 的典型特征：不可复现不代表不存在，和“偶发 NPE/脏读”一样。

## 复盘模板（建议写在笔记里）

- 本章一句话：`volatile` 到底提供了哪三类语义（可见性/有序性/发布），以及明确“不提供什么”（原子性）。
- 实验 A 的核心对比点是什么？为什么非 `volatile` 可能“停不下来”？
- 实验 B 为什么会丢失更新？用一句话说清 `++` 的读-改-写。
- 实验 C 的 `(0,0)` 直觉是什么？`volatile` 为什么能禁止它（从 happens-before 角度回答）。
- 实验 D 的“安全发布”要解决什么风险？如何在工程里选择更合适的手段（`final`/锁/不可变对象/安全发布容器等）。
