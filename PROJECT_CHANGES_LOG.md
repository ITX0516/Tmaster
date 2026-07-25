# Tmaster 项目接手改动日志

> 生成时间: 2026-07-25
> 接手人: AI Assistant
> 项目: https://github.com/ITX0516/Tmaster
> 目标: 修复引擎启动失败和闪退问题，添加文件日志系统

---

## 一、项目架构概述

### 技术栈
- **语言**: Kotlin + Java (Android)
- **UI**: Jetpack Compose (Material3)
- **数据库**: Room
- **网络**: gRPC + OkHttp
- **引擎**: KataGo (通过 Go JNI 桥接)
- **构建**: Gradle 8.7 + GitHub Actions CI

### 关键目录结构
```
app/src/main/
├── java/com/tmaster/
│   ├── TmasterApp.kt          # Application 入口
│   ├── ui/                    # Compose UI
│   ├── engine/                # 引擎管理 (KataGoSdkEngine, EngineManager, ModelManager)
│   ├── game/                  # 围棋逻辑
│   ├── data/                  # Room 数据库
│   └── log/                   # 日志系统 (新增)
├── java/ikatagosdk/           # JNI SDK 封装
│   ├── Client.kt
│   ├── KatagoRunner.kt
│   ├── Ikatagosdk.kt
│   ├── NativeLoader.kt        # Native 库加载管理 (新增)
│   └── ...
├── jniLibs/arm64-v8a/         # Native .so 库
│   ├── libgojni.so            # Go JNI 桥 (包含所有 JNI 方法)
│   ├── libkatago.so           # KataGo C++ 库 (有 JNI_OnLoad 但返回 JNI_ERR)
│   └── libc++_shared.so
└── assets/katago.cfg          # KataGo 配置文件
```

---

## 二、问题诊断与修复时间线

### Phase 1: JNI "No implementation found" 错误

**症状**: `JNI createKatagoRunner: No implementation found for ikatagosdk.KatagoRunner ikatagosdk.Client.createKatagoRunner()`

**根因**: Kotlin `companion object` 的 `init` 块是惰性初始化的。类实例化不会触发伴生对象初始化，导致 native 库未加载就调用 `external` 方法。

**修复**:
1. **新增 [NativeLoader.kt](app/src/main/java/ikatagosdk/NativeLoader.kt)**
   - 用 `object` 单例管理 native 库加载
   - `@Synchronized` 线程安全
   - 记录加载错误避免重复尝试

2. **修改 [Client.kt](app/src/main/java/ikatagosdk/Client.kt)**
   - 在 `init` 块中调用 `NativeLoader.ensureLoaded()`

3. **修改 [KatagoRunner.kt](app/src/main/java/ikatagosdk/KatagoRunner.kt)**
   - 在 `init` 块中调用 `NativeLoader.ensureLoaded()`

### Phase 2: JNI_OnLoad 失败

**症状**: `JNI_ERR returned from JNI_OnLoad in ".../libkatago.so"`

**根因分析** (通过 readelf/nm 工具):
- `libkatago.so` 有 `JNI_OnLoad` 但返回 `JNI_ERR`
- `libgojni.so` (Go 编译) **不依赖** `libkatago.so` (readelf 无 DT_NEEDED)
- `libgojni.so` 包含所有 `Java_ikatagosdk_*` JNI 方法
- `libgojni.so` 内部用 `dlopen` 独立管理 KataGo

**修复**:
- **修改 [NativeLoader.kt](app/src/main/java/ikatagosdk/NativeLoader.kt)**: 只加载 `libgojni.so`，不加载 `libkatago.so`

### Phase 3: 应用闪退 (SIGSEGV 信号崩溃)

**症状**: 日志在 `client.createKatagoRunner()` 后截断，无异常堆栈

**根因分析** (通过 nm 分析 native 符号):
- `libgojni.so` **没有** `Java_ikatagosdk_Client__init` 构造函数符号
- `libgojni.so` **有** `Java_ikatagosdk_Ikatagosdk_newClient` 工厂方法符号
- 直接 `Client("", "local", "", "")` 构造的对象 native peer 为 null
- 调用 `createKatagoRunner()` 时解引用空指针 → SIGSEGV

**修复**:
- **修改 [KataGoSdkEngine.kt](app/src/main/java/com/tmaster/engine/KataGoSdkEngine.kt)**:
  - 改用 `Ikatagosdk.newClient()` 工厂方法创建 Client
  - 添加 `Ikatagosdk._init()` SDK 初始化调用
  - 每个 native 调用前后 `FileLogger.flush()` 确保日志落盘

### Phase 4: 日志系统搭建

**背景**: 用户无法看到闪退原因，因为 logcat 在 0.5s 内来不及查看

**新增文件**:

1. **[FileLogger.kt](app/src/main/java/com/tmaster/log/FileLogger.kt)**
   - 异步线程写入日志文件
   - 2MB 日志自动轮转，保留 3 个备份
   - 日志路径: `/storage/emulated/0/Android/data/com.tmaster.debug/log/log.txt`
   - 同步/异步双模式写入

2. **[CrashHandler.kt](app/src/main/java/com/tmaster/log/CrashHandler.kt)**
   - 全局未捕获异常处理器
   - 崩溃前 flush 日志并等待 500ms

3. **[TLogger.kt](app/src/main/java/com/tmaster/log/TLogger.kt)**
   - 封装 `Log.xxx()` + `FileLogger.xxx()` + `LogCollector`
   - `ModuleLogger` 便捷类

4. **[LogViewerScreen.kt](app/src/main/java/com/tmaster/ui/settings/LogViewerScreen.kt)**
   - Compose 日志查看界面
   - 显示最近 500 行，支持刷新/清空

5. **修改 [SettingsScreen.kt](app/src/main/java/com/tmaster/ui/settings/SettingsScreen.kt)**
   - 添加"查看日志"入口

### Phase 5: 权重文件路径迁移

**背景**: 权重文件存在内部存储 `/data/user/0/...`（需要 root），用户无法查看

**修复**:
- **修改 [ModelManager.kt](app/src/main/java/com/tmaster/engine/ModelManager.kt)**: 权重目录改为 `getExternalFilesDir(null)/katago/weights`
- **修改 [EngineManager.kt](app/src/main/java/com/tmaster/engine/EngineManager.kt)**: 配置文件改为 `getExternalFilesDir(null)/katago/katago.cfg`
- **修改 [TmasterApp.kt](app/src/main/java/com/tmaster/TmasterApp.kt)**: 预提取路径同步修改

现在所有文件都在外部存储:
- 日志: `/storage/emulated/0/Android/data/com.tmaster.debug/log/log.txt`
- 权重: `/storage/emulated/0/Android/data/com.tmaster.debug/files/katago/weights/`
- 配置: `/storage/emulated/0/Android/data/com.tmaster.debug/files/katago/katago.cfg`

### Phase 6: Application 启动优化

**修改 [TmasterApp.kt](app/src/main/java/com/tmaster/TmasterApp.kt)**:
- 添加 `attachBaseContext()` 在最早阶段初始化 FileLogger
- 所有初始化步骤拆分为独立 try-catch，不让一个失败拖垮全部
- 添加 `FileLogger.flush()` 在关键步骤后强制落盘
- 后台线程预提取权重文件，避免主线程 ANR

### Phase 7: GitHub Actions CI 修复

**修改 [.github/workflows/ci.yml](.github/workflows/ci.yml)**:
- 添加 `workflow_dispatch` 手动触发
- 修复 Gradle Wrapper 缺失问题 (使用 `gradle/actions/setup-gradle@v4`)
- 修复 APK 上传路径

---

## 三、当前代码关键逻辑

### Native 库加载流程
```
TmasterApp.attachBaseContext()
  └─ FileLogger.init() ── 初始化文件日志

TmasterApp.onCreate()
  └─ CrashHandler.init()
  └─ ModelManager() 初始化

用户点击"开始对弈"
  └─ EngineManager.setup()
      └─ KataGoSdkEngine.initialize()
          └─ Ikatagosdk._init()           ← SDK 初始化
          └─ Ikatagosdk.newClient()       ← 工厂方法创建 Client
          └─ client.createKatagoRunner()  ← 创建 Runner
          └─ runner.setKataName/Config/Weight/...  ← 配置
          └─ runner.run()                 ← 启动引擎
          └─ sendGtp("boardsize 19")      ← GTP 初始化
```

### 关键类关系
```
Ikatagosdk (object)
  ├─ _init()
  ├─ newClient(): Client
  └─ newClientRunnerFromArgs(): ClientRunner

Client (class)
  ├─ init { NativeLoader.ensureLoaded() }
  └─ createKatagoRunner(): KatagoRunner

KatagoRunner (class)
  ├─ init { NativeLoader.ensureLoaded() }
  ├─ run(): Boolean
  ├─ sendGTPCommand(cmd): String
  └─ setKataWeight(weightDir, configPath)

NativeLoader (object)
  └─ ensureLoaded() → System.loadLibrary("gojni")
```

---

## 四、仍存在的问题 / 待验证

1. **引擎能否正常对弈**: 已通过 `Ikatagosdk.newClient()` 修复了构造问题，但 `runner.run()` 和 GTP 交互尚未在实机验证
2. **libkatago.so 的作用**: 文件存在但未被 Java 层加载，可能由 libgojni.so 内部 dlopen 使用。如果 gojni 内部找不到 katago 可能仍会崩溃
3. **权重文件格式**: res/raw 中的权重是 GZIP 压缩的，解压后约为 5-17MB/个，解压过程在后台线程
4. **数据库路径**: Room 数据库仍在内部存储，不影响功能
5. **CI 构建队列**: GitHub Actions 有时排队较久（5-10 分钟）

---

## 五、GitHub 仓库状态

- **分支**: master
- **CI 状态**: 构建成功 (GitHub Actions)
- **APK 下载**: https://github.com/ITX0516/Tmaster/actions → 最新 successful run → Artifacts
- **Token**: 用户提供了 GitHub PAT (用于 Actions 触发，不在文档中记录)

---

## 六、给下一个 AI 的建议

1. **如果用户报告闪退**: 先看 `/storage/emulated/0/Android/data/com.tmaster.debug/log/log.txt`
2. **如果日志在 createKatagoRunner() 截断**: 说明 native 层崩溃，需要分析 native 代码或 .so 依赖
3. **如果日志显示 "run() = false"**: 说明 runner.run() 返回失败，检查权重文件路径和 katago.cfg 配置
4. **如果需要修改 native 加载**: 只改 `NativeLoader.kt`，**不要**加载 libkatago.so
5. **文件路径统一原则**: 权重/配置优先用 `getExternalFilesDir(null)`，日志用 `getExternalFilesDir(null).parentFile/log/`

---

## 七、修改文件清单 (按模块)

### 新增文件
| 文件 | 作用 |
|------|------|
| `app/src/main/java/ikatagosdk/NativeLoader.kt` | Native 库统一管理 |
| `app/src/main/java/com/tmaster/log/FileLogger.kt` | 文件日志写入 |
| `app/src/main/java/com/tmaster/log/CrashHandler.kt` | 全局崩溃捕获 |
| `app/src/main/java/com/tmaster/ui/settings/LogViewerScreen.kt` | 日志查看界面 |

### 修改文件
| 文件 | 修改内容 |
|------|----------|
| `app/src/main/java/ikatagosdk/Client.kt` | init 块添加 NativeLoader.ensureLoaded() |
| `app/src/main/java/ikatagosdk/KatagoRunner.kt` | init 块添加 NativeLoader.ensureLoaded() |
| `app/src/main/java/ikatagosdk/Ikatagosdk.kt` | 无修改，作为工厂方法入口 |
| `app/src/main/java/com/tmaster/TmasterApp.kt` | attachBaseContext 初始化日志，拆分 try-catch，后台提取权重 |
| `app/src/main/java/com/tmaster/engine/KataGoSdkEngine.kt` | 改用 Ikatagosdk.newClient()，添加 logSync，每步 flush |
| `app/src/main/java/com/tmaster/engine/ModelManager.kt` | 权重目录改外部存储 |
| `app/src/main/java/com/tmaster/engine/EngineManager.kt` | 配置文件改外部存储 |
| `app/src/main/java/com/tmaster/log/TLogger.kt` | 添加 FileLogger 输出 |
| `app/src/main/java/com/tmaster/ui/settings/SettingsScreen.kt` | 添加日志查看入口 |
| `.github/workflows/ci.yml` | 修复 Gradle Wrapper，添加 workflow_dispatch |
