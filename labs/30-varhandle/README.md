# 第 30 章实验：VarHandle 与内存语义 #

本章目标：用两个最小实验把 VarHandle 的两类核心能力跑出来：**原子更新（CAS）**与**发布-获取（acquire/release）**。

## 编译

先确认你使用的是 JDK 21：

```powershell
java -version
```

```powershell
cd labs\30-varhandle
javac *.java
```

## 实验 A：CAS 计数器（`VarHandleAtomicDemo`）

先跑一组默认参数：

```powershell
java VarHandleAtomicDemo
```

再跑一组更大的压力（你也可以重复 3 次取中位数）：

```powershell
java VarHandleAtomicDemo 8 500000
```

观察点：

- `expected` 是否总是等于 `actual`（验证 CAS 的原子性）
- 线程数/迭代次数上去后耗时如何变化（自旋重试会带来成本）

## 实验 B：acquire/release 的发布-获取关系（`VarHandleAcquireReleaseDemo`）

```powershell
java VarHandleAcquireReleaseDemo
```

观察点：

- 程序是否能稳定结束（没有卡死）
- 输出的 `lastObserved` 是否在不断变化（重点是理解“发布-获取协议”，不是追求某个固定值）

注意：

- “内存可见性问题”往往是**不稳定且难复现**的；这个实验更适合用来理解 acquire/release 的写法与协议位置，而不是指望每次都能跑出“错误示例”。

## 复盘模板（建议写在笔记里）

- 本章一句话：VarHandle 解决了什么问题？相比 Atomic* 多了什么能力？
- 访问模式排序：plain / opaque / acquire-release / volatile 分别在控制什么？
- 协议表达：为什么“先写 data，再 release 写 flag；先 acquire 读 flag，再读 data”是一个关键模式？
- 工程取舍：什么情况下应该坚持用 Atomic*，什么情况下才考虑 VarHandle？
