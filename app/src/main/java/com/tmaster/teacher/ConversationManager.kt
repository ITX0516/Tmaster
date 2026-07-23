package com.tmaster.teacher

import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 对话管理器 — AI 老师的核心引擎。
 *
 * 参考 GoAgent ConversationManager 设计:
 * - 维护对话历史 → 自动截断超长消息
 * - 工具调用循环 → LLM 可多次调用工具直到给出最终答案
 * - 多模态 → 棋盘截图 + 分析数据 + 知识卡 + 学生画像
 * - Token 统计 → 每次调用返回用量
 *
 * 循环流程:
 * 1. 组装上下文 (棋盘 + KataGo + 知识卡 + 学生画像)
 * 2. 发送给 LLM
 * 3. LLM 返回 text → 流式输出
 * 4. LLM 返回 tool_calls → 执行工具 → 结果追加到历史 → 回到步骤 2
 * 5. 最多 5 轮工具调用防止死循环
 */
class ConversationManager(
    private val provider: LlmProvider,
    private val toolRegistry: ToolRegistry,
    private val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    private val maxToolRounds: Int = 5,
) {
    private val logger = ModuleLogger("ConvMgr")
    private val history = mutableListOf<Map<String, Any?>>()

    /** 清空对话历史。 */
    fun reset() {
        history.clear()
        history.add(mapOf("role" to "system", "content" to systemPrompt))
        logger.d("conversation reset")
    }

    /**
     * 发送消息 → 流式返回 AI 老师的回答。
     * 每个 chunk 是一个回答片段，UI 可以逐步显示。
     */
    fun chat(
        userMessage: String,
        imageBase64: String? = null,
    ): Flow<String> = flow {
        if (history.isEmpty()) reset()

        // 构建用户消息
        val userMsg = if (imageBase64 != null) {
            buildMultiModalMessage(userMessage, imageBase64)
        } else {
            mapOf("role" to "user", "content" to userMessage)
        }
        history.add(userMsg)

        // Agent 循环
        for (round in 0..maxToolRounds) {
            val tools = toolRegistry.toToolDefs()
            val response = provider.chat(
                messages = history.toList(),
                tools = if (tools.isNotEmpty()) tools else null,
            )

            // Token 统计
            if (response.usage != null) {
                logger.d("tokens: ${response.usage.totalTokens} (prompt=${response.usage.promptTokens})")
            }

            // LLM 想调用工具
            if (response.toolCalls != null && response.toolCalls.isNotEmpty()) {
                history.add(buildAssistantToolCalls(response.toolCalls!!))

                for (tc in response.toolCalls!!) {
                    val tool = toolRegistry.get(tc.name)
                    val result = if (tool != null) {
                        try {
                            tool.execute(tc.arguments)
                        } catch (e: Exception) {
                            logger.e("tool ${tc.name} error: ${e.message}")
                            "Error: ${e.message}"
                        }
                    } else {
                        "Unknown tool: ${tc.name}"
                    }
                    history.add(mapOf(
                        "role" to "tool",
                        "tool_call_id" to tc.id,
                        "content" to result,
                    ))
                }
                continue // 继续循环，让 LLM 看到工具结果
            }

            // LLM 给出文本回答
            if (!response.text.isNullOrBlank()) {
                history.add(mapOf("role" to "assistant", "content" to response.text))
                emit(response.text)
            }
            return@flow
        }
        emit("（分析过程较长，请重新提问）")
    }

    private fun buildMultiModalMessage(text: String, imageBase64: String): Map<String, Any> {
        return mapOf(
            "role" to "user",
            "content" to listOf(
                mapOf("type" to "text", "text" to text),
                mapOf(
                    "type" to "image_url",
                    "image_url" to mapOf("url" to "data:image/png;base64,$imageBase64"),
                ),
            ),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildAssistantToolCalls(calls: List<ToolCall>): Map<String, Any?> {
        return mapOf(
            "role" to "assistant",
            "content" to null,
            "tool_calls" to calls.map { tc ->
                mapOf(
                    "id" to tc.id,
                    "type" to "function",
                    "function" to mapOf(
                        "name" to tc.name,
                        "arguments" to tc.arguments.toString(), // simplified
                    ),
                )
            },
        )
    }

    companion object {
        val DEFAULT_SYSTEM_PROMPT = """
你是一位专业的围棋老师。你的任务是将 KataGo 的分析结果转化为学生能听懂、能执行的复盘建议。

## 你的原则
1. 耐心、鼓励。每次只讲 1-2 个关键点，不要信息过载。
2. 解释"为什么"这步好或不好，而不只是"这步好/不好"。
3. 用 KataGo 分析数据支撑你的解释（胜率变化、目差、变化图）。
4. 参考知识卡给学生推荐学习资源。
5. 根据学生水平和最近弱点调整讲解深度。
6. 学生有具体问题时，先直接回答，再展开。
7. 结尾给出一个"本周可以练的具体方向"。

## 格式
- 先简要评价局面
- 指出关键决策点
- 解释发生了什么、为什么
- 展示更好的下法（如果有）
- 推荐一个具体练习方向
        """.trimIndent()
    }
}
