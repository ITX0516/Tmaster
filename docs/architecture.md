# 架构设计

## 分层架构

```
┌─────────────────────────────────────────────────┐
│                 Presentation                     │
│  BoardWidget  GameScreen  AnalysisScreen        │
│  TeacherChat  Settings     LibraryScreen        │
├─────────────────────────────────────────────────┤
│                    Domain                       │
│  Entities: GameState, Move, AnalysisResult      │
│  UseCases: PlayGame, AnalyzeGame, TeachPosition │
│  Repositories (interfaces): GameRepo, ProfileRepo│
├─────────────────────────────────────────────────┤
│                    Data                         │
│  SQLite (games, profiles, knowledge cards)       │
│  FileSystem (SGF, engine weights)               │
│  gRPC/WebSocket (remote engines)                │
│  LLM API (OpenAI / Anthropic)                   │
├─────────────────────────────────────────────────┤
│                   Engine                        │
│  GoEngine interface                             │
│  ├── Local: KataGo FFI, Leela Zero FFI          │
│  └── Remote: gRPC proxy to cloud/PC host        │
└─────────────────────────────────────────────────┘
```

## 引擎接口

```dart
abstract class GoEngine {
  Future<void> initialize(EngineConfig config);
  Stream<AnalysisResult> analyze(BoardState state, {...});
  Future<Move> generateMove(BoardState state, {...});
  Future<void> dispose();
}
```

### 本地引擎

- KataGo: 通过 Rust FFI 层调用 C++ 库
- Leela Zero: 同样通过 FFI
- 编译目标: ARM64 (Android NDK / iOS)

### 远程引擎

- 协议: gRPC bidirectional stream
- 平台: 智星云、算云、个人电脑
- 连接字符串: `host:port` 或 `wss://cloud-platform.com`

### 引擎选择流程

```
用户选择 → EngineConfig.type
  ├── local  → 检查本地权重文件 → 初始化 FFI → ready
  └── remote → 检查网络连接 → gRPC handshake → 鉴权 → ready
```

## LLM Agent 工具系统

类似 MCP (Model Context Protocol)，Agent 可以调用以下工具：

| 工具 | 用途 |
|------|------|
| `library.findGames` | 按棋手/来源/日期搜索棋谱库 |
| `sgf.readGameRecord` | 读取 SGF 主线、棋局信息 |
| `katago.analyzePosition` | 分析当前局面 |
| `katago.analyzeGameBatch` | 批量分析一盘或多盘棋 |
| `board.captureTeachingImage` | 生成带坐标和推荐点的棋盘截图 |
| `knowledge.searchLocal` | 检索本地围棋知识卡 |
| `studentProfile.read` | 读取学生画像 |
| `studentProfile.write` | 更新学生画像 |
| `report.saveAnalysis` | 保存分析报告 |

### Agent 执行循环

```
用户提问 / 自动触发
  ↓
组装 AgentContext (棋盘图 + 分析数据 + 知识卡 + 学生画像)
  ↓
发送给 LLM (带 tool definitions)
  ↓
LLM 返回 text 或 tool_calls
  ├── text → 流式输出给用户
  └── tool_calls → 执行工具 → 结果追加到消息历史 → 继续循环 (最多 5 轮)
```

## 分析模式

### Lizzie 风格体验

1. 加载棋谱后**默认自动开始** KataGo 分析
2. 切换手数后**自动继续分析**当前局面
3. 只有用户点击暂停，分析才停止
4. 推荐点显示：选点序号、胜率、目差、搜索数
5. 实战下一手和 AI 推荐对照，问题手按胜率/目差损失判断
6. 鼠标悬停（移动端：长按）推荐点展示后续变化

### 鹰眼分析流程

```
加载 SGF → 解析所有手数
  ↓
逐手分析 (自动推进)
  ├── 每手调用 engine.analyze()
  ├── 比较实战手 vs AI 推荐
  ├── 胜率损失 > 阈值 → 标记为问题手
  └── 实战手 == AI 首选 → AI 匹配 +1
  ↓
汇总: 问题手列表 + AI 重合度 + 胜率曲线
```

## 对弈模式

### 棋风类型
- 均衡 (balanced): 默认 KataGo 设置
- 激进 (aggressive): 高 temperature, 倾向复杂变化
- 稳健 (solid): 低 temperature, 倾向简化
- 自定义: 用户调整参数

### 难度等级
- 引擎通过限制 visits/playouts 模拟不同水平
- 18k ~ 9d 共 38 个等级映射到不同的 visit 上限

## SGF 导入/导出

- 导入: 文件选择器 → sgf_parser → BoardState → 可继续对弈或分析
- 导出: BoardState → SGF 节点树 → sgf_writer → 文件
- 支持分支 (variations)、注释 (comments)、标记 (marks)

## 本地存储

### SQLite 表设计

```sql
-- 棋谱库
CREATE TABLE games (
  id TEXT PRIMARY KEY,
  sgf_data TEXT NOT NULL,
  black_player TEXT,
  white_player TEXT,
  result TEXT,
  date_played TEXT,
  source TEXT,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 分析结果
CREATE TABLE analysis_results (
  id TEXT PRIMARY KEY,
  game_id TEXT REFERENCES games(id),
  move_number INTEGER,
  analysis_json TEXT NOT NULL,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 学生画像
CREATE TABLE student_profile (
  id TEXT PRIMARY KEY,
  profile_json TEXT NOT NULL,
  updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- 知识卡
CREATE TABLE knowledge_cards (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  tags TEXT,       -- JSON array
  category TEXT,
  level TEXT,
  created_at TEXT DEFAULT CURRENT_TIMESTAMP
);
```
