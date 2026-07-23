package com.tmaster.engine

/**
 * GTP (Go Text Protocol) 命令构建与响应解析。
 *
 * KataGo 通过 GTP 协议通信，这是所有围棋 AI 的行业标准。
 * 文档: https://www.lysator.liu.se/~gunnar/gtp/
 */
object GtpProtocol {

    // ── 命令构建 ────────────────────────────────────────────

    fun version() = "version"
    fun name() = "name"
    fun boardsize(n: Int) = "boardsize $n"
    fun clearBoard() = "clear_board"
    fun komi(k: Double) = "komi $k"
    fun play(color: String, coord: String) = "play $color $coord"
    fun genmove(color: String) = "genmove $color"

    /**
     * 设置 KataGo 分析参数 (非标准 GTP，KataGo 扩展)。
     */
    fun kataAnalyze(
        interval: Int = 100,    // 多少毫秒返回一次中间结果
        maxVisits: Int? = null,
    ): String {
        val opts = mutableListOf("interval $interval")
        if (maxVisits != null) opts.add("maxvisits $maxVisits")
        return "kata-analyze ${opts.joinToString(" ")}"
    }

    fun kataStop() = "kata-stop"

    /** 配置 KataGo 规则（中国规则）。 */
    fun kataSetRule(chinese: Boolean = true): String {
        return if (chinese) "kata-set-rules chinese" else "kata-set-rules japanese"
    }

    // ── 响应解析 ────────────────────────────────────────────

    /** 解析 GTP 标准响应，返回 "=" 后面的内容。 */
    fun parseResponse(raw: String): GtpResponse {
        if (raw.isEmpty()) return GtpResponse.Error("empty response")
        return when (raw[0]) {
            '=' -> GtpResponse.Success(raw.substring(1).trim())
            '?' -> GtpResponse.Error(raw.substring(1).trim())
            else -> GtpResponse.Error("unexpected response: $raw")
        }
    }

    /**
     * 解析 KataGo 分析响应 JSON。
     *
     * 格式: {"id":"...","isDuringSearch":true/false,"moveInfos":[...],"rootInfo":{...}}
     */
    fun parseKataAnalysis(json: String): KataAnalysis {
        // 简化解析 — 完整版需要 Moshi 反序列化
        // moveInfos 的每个元素: {"move":"pd","visits":1234,"winrate":0.52,"scoreLead":1.5,...}
        return KataAnalysis(json) // 实际由 KotlinX Serialization 处理
    }
}

sealed class GtpResponse {
    data class Success(val content: String) : GtpResponse()
    data class Error(val message: String) : GtpResponse()
}

/**
 * KataGo 分析结果原始容器 — 完整反序列化在具体 Provider 中处理。
 */
data class KataAnalysis(val rawJson: String)
