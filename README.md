# Tmaster — 手机围棋 AI 教练

KataGo + LLM 驱动的新一代围棋学习工具。KataGo 是事实裁判，LLM 是讲棋老师。

## 核心能力

| 功能 | 说明 |
|------|------|
| AI 对弈 | 本地 KataGo（15b/8b 两档），离线可用；远程 GPU 算力 |
| 鹰眼全谱分析 | 问题手、好手、AI 重合度 — 逐手对照 KataGo 推荐 |
| AI 老师 | LLM 智能体，工具调用，非固定模板，个性化教学 |
| 多模态讲棋 | 棋盘截图 + KataGo JSON + 知识卡 + 学生画像 → LLM |
| 类 Lizzie 体验 | 加载自动分析、暂停/继续、手数切换即分析 |
| SGF 支持 | 导入/导出，变招树，继续对局 |
| 3 种落子 | 单击 / 双击 / 确认按钮 |
| 让子 + 计时 | 标准让子位置 + 读秒/包干/加拿大式 |

## 技术栈

```
语言:    Kotlin
UI:      Jetpack Compose + Canvas
引擎:    KataGo C++ → NDK → JNI → GTP 协议
远程:    gRPC stream → 智星云 / 算云 / 个人电脑
LLM:     ConversationManager + Tool Registry (参考 GoAgent)
本地DB:  Room (SQLite)
模型:    15 block (默认) + 8 block (低配回退)
```

## 项目结构

```
Tmaster/
├── app/src/main/java/com/tmaster/
│   ├── engine/          ← GTP 协议 + KataGo JNI + 远程引擎 + 模型管理
│   ├── game/            ← 棋盘状态 + SGF 解析 + 让子 + 计时器
│   ├── analysis/        ← 鹰眼分析器 + 问题手/好手检测
│   ├── teacher/         ← LLM Provider + Tool Registry + ConversationManager
│   ├── data/            ← Room 数据库 + Repository
│   ├── ui/              ← Compose: 棋盘 / 对弈 / 分析 / AI老师 / 棋谱库
│   ├── log/             ← 统一日志系统
│   └── error/           ← 异常分类 + 用户友好报错
├── .github/workflows/   ← CI: Android APK 构建
└── README.md
```

## 数据流

```
SGF → BoardState → KataGo.analyze() → AnalysisResult
                        ↓
              HawkEyeAnalyzer
                        ↓
          GameAnalysis (问题手 + 好手 + AI 重合度)
                        ↓
          ConversationManager (棋盘截图 + 分析 + 知识卡 + 学生画像)
                        ↓
          AI 老师 → 流式教学文本
```
