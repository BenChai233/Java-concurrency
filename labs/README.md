# Labs（可运行示例）

本目录用于存放与各章节配套的最小可运行实验（MRE），帮助把“概念”落到“可观察的现象”。

## 环境

- JDK：21+
- 不依赖 Maven/Gradle，直接使用 `javac`/`java`

## 运行方式（Windows PowerShell）

先确认你使用的是 JDK 21：

```powershell
java -version
```

示例（第 1 章）：

```powershell
cd labs\01-concurrency-pros-cons
javac *.java
java ConcurrencyBenefits --mode io --tasks 200 --threads 8 --sleepMs 20
java RaceConditionAndFixes --threads 8 --iterations 2_000_000
```

## 约定

- 每章一个目录：`labs/01-...`、`labs/02-...`……
- 每个实验尽量只展示 1 个结论，避免把多个变量搅在一起
- 输出尽量包含：参数、耗时、吞吐/正确性校验（便于复盘）
