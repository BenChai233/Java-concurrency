# 06-final

本实验为第 06 章（final）对应实验。

## 如何运行

在本目录执行：

1) 编译：
   javac *.java

2) 运行：
   java FinalFieldVisibilityDemo [iterations]
   java FinalReferenceMutabilityDemo
   java FinalConfigShareDemo [threads]
   java FinalThisEscapeDemo [iterations]

## 实验说明

FinalFieldVisibilityDemo
- 目标：比较 final 与普通字段的可见性（非安全发布场景）。
- 观察：普通字段可能出现默认值；final 更安全但仍具概率性。

FinalReferenceMutabilityDemo
- 目标：验证 final 引用不等于对象不可变。
- 观察：final 数组/集合内容仍可修改。

FinalConfigShareDemo
- 目标：验证 final 不可变配置的并发只读安全。
- 观察：多线程读取一致值，无写入竞争。

FinalThisEscapeDemo
- 目标：演示构造期间 this 逃逸导致的可见性问题。
- 观察：普通字段可能出现默认值；final 也可能受影响（概率性）。
