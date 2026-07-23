# Tmaster — 手机围棋AI教练

## 项目概述

一款移动端围棋软件，集成本地/远程 AI 引擎（KataGo、Leela Zero），搭配 LLM 智能体作为围棋老师，提供从分析到教学的全流程体验。

## 核心能力

| 功能 | 说明 |
|------|------|
| AI 对弈 | 本地 KataGo / Leela Zero，离线可用；支持远程 GPU 算力 |
| 鹰眼分析 | 全谱分析：问题手、好手、AI 重合度 |
| AI 老师 | LLM 智能体，不是固定模板，可以调用工具进行个性化教学 |
| 多模态讲棋 | 棋盘截图 + KataGo 数据 + 知识卡 + 学生画像 → LLM 生成教学 |
| SGF 支持 | 导入/导出，继续对局 |
| 3 种落子 | 单击、双击、确认按钮 |

## 技术选型

- **移动端**: Flutter (Dart) — 跨平台 + 高性能棋盘渲染
- **AI 引擎**: KataGo C++ → ARM 编译 (NDK) + FFI 桥接
- **远程计算**: gRPC stream 连接云 GPU / 个人电脑
- **LLM 集成**: 多模态 provider 抽象层，支持 OpenAI / Anthropic / 本地模型
- **本地存储**: SQLite (游戏记录、学生画像、知识卡)
- **棋盘逻辑**: 纯 Dart 实现 (零依赖)

## 项目结构

```
Tmaster/
├── packages/                    # 独立 Dart 包（按依赖分层）
│   ├── go_board/                # 围棋规则引擎
│   ├── sgf_parser/              # SGF 解析/序列化
│   ├── analysis_core/           # AI 引擎接口 + 鹰眼分析
│   └── llm_agent/               # LLM 智能体框架
│
├── app/                         # Flutter 移动端应用
│   └── lib/
│       ├── core/                # DI, 配置, 主题, 工具
│       ├── domain/              # 实体, 值对象, 用例, 仓库接口
│       ├── data/                # 数据层实现 (DB, API, 文件)
│       ├── engine/              # 引擎层 (本地/远程实现)
│       └── presentation/        # UI 层 (棋盘, 分析, 对弈, 教学)
│
├── native/                      # 原生引擎绑定
│   ├── katago/                  # KataGo JNI 桥接
│   └── leela_zero/              # Leela Zero JNI 桥接
│
└── docs/                        # 文档
    └── architecture.md
```

## 数据流

```
SGF 文件 → sgf_parser → BoardState
                            ↓
     ┌─→ GoEngine.analyze() → AnalysisResult
     │         ↓
     │   HawkEyeAnalyzer → GameAnalysisSummary
     │         ↓
     │   AgentContext (board image + analysis + knowledge + profile)
     │         ↓
     └─→ GoTeacherAgent.teach() → Stream<String> (教学文本)
```

## 包依赖关系

```
go_board (无依赖)
  ↑
sgf_parser → go_board
  ↑
analysis_core → go_board
  ↑
llm_agent → go_board + analysis_core
  ↑
app → go_board + sgf_parser + analysis_core + llm_agent
```
