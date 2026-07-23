package com.tmaster.teacher

/**
 * LLM Provider 抽象接口 — 参考 GoAgent 的统一设计。
 * 支持 OpenAI / Anthropic / DeepSeek / 通义千问 等所有主流厂商。
 */
interface LlmProvider {
    val providerId: String
    val modelId: String

    /**
     * 发送聊天请求。
     *
     * @param messages 消息历史 (role + content)
     * @param tools 可用工具定义 (null = 不传 tools)
     * @param imageBase64 棋盘截图 (多模态，null = 纯文本)
     * @return 文本内容 + 可能的工具调用
     */
    suspend fun chat(
        messages: List<Map<String, Any>>,
        tools: List<ToolDef>? = null,
        imageBase64: String? = null,
    ): LlmResponse
}

data class LlmResponse(
    val text: String?,
    val toolCalls: List<ToolCall>?,
    val usage: Usage?,
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>,
)

data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

/**
 * 工具定义 (传给 LLM 的 function schema)。
 */
data class ToolDef(
    val name: String,
    val description: String,
    val parameters: Map<String, ToolParam>,
)

data class ToolParam(
    val type: String,
    val description: String,
    val enum: List<String>? = null,
)
