package com.tmaster.teacher

import com.tmaster.log.ModuleLogger

/**
 * 工具注册表 — LLM 可以调用的所有围棋教学工具。
 *
 * 参考 GoAgent 的 RegisterFunction 模式，每个工具是 (name, desc, execute) 三元组。
 */
class ToolRegistry {
    private val logger = ModuleLogger("ToolReg")
    private val tools = mutableMapOf<String, RegisteredTool>()

    fun register(tool: RegisteredTool) {
        tools[tool.name] = tool
        logger.d { "registered tool: ${tool.name}" }
    }

    fun get(name: String): RegisteredTool? = tools[name]
    fun all(): List<RegisteredTool> = tools.values.toList()

    /** 生成所有工具的 LLM function schema 列表。 */
    fun toToolDefs(): List<ToolDef> = tools.values.map { tool ->
        ToolDef(
            name = tool.name,
            description = tool.description,
            parameters = tool.params.mapValues { (_, v) ->
                ToolParam(type = v.type, description = v.desc, enum = v.enum)
            },
        )
    }
}

data class RegisteredTool(
    val name: String,
    val description: String,
    val params: Map<String, ToolParamMeta>,
    val execute: suspend (Map<String, Any>) -> String,
)

data class ToolParamMeta(
    val type: String,     // "string" | "number" | "integer" | "boolean" | "array"
    val desc: String,
    val required: Boolean = true,
    val enum: List<String>? = null,
)
