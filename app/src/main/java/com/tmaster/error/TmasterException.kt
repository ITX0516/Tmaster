package com.tmaster.error

/**
 * 异常基类 — 所有 Tmaster 内部错误继承此类。
 * [code] 为唯一错误码，[userMessage] 是对用户友好的中文说明。
 */
sealed class TmasterException(
    override val message: String,
    val code: Int,
    val userMessage: String,
) : Exception(message) {

    // ── 引擎异常 ──────────────────────────────────────────────

    class EngineNotFound(path: String) : TmasterException(
        message = "Engine binary not found: $path",
        code = 1001,
        userMessage = "未找到 KataGo 引擎文件，请检查应用安装是否完整",
    )

    class EngineCrashed(exitCode: Int, stderr: String) : TmasterException(
        message = "Engine crashed (exit=$exitCode): $stderr",
        code = 1002,
        userMessage = "KataGo 引擎异常退出，正在尝试重启...",
    )

    class ModelNotLoaded(path: String) : TmasterException(
        message = "Model weights not found: $path",
        code = 1003,
        userMessage = "未找到 AI 模型文件，请下载模型后再试",
    )

    class GtpTimeout(command: String) : TmasterException(
        message = "GTP command timed out: $command",
        code = 1004,
        userMessage = "引擎响应超时，请检查设备性能或降低分析深度",
    )

    class EngineBusy : TmasterException(
        message = "Engine is already processing a request",
        code = 1005,
        userMessage = "引擎正忙，请稍后再试",
    )

    // ── 网络异常 ──────────────────────────────────────────────

    class RemoteConnectionFailed(host: String, cause: Throwable?) : TmasterException(
        message = "Connection to $host failed",
        code = 2001,
        userMessage = "无法连接到远程引擎 $host，请检查网络和地址",
    )

    class RemoteAuthFailed(host: String) : TmasterException(
        message = "Authentication failed for $host",
        code = 2002,
        userMessage = "远程引擎认证失败，请检查密钥",
    )

    class LlmApiError(provider: String, httpCode: Int, body: String) : TmasterException(
        message = "$provider API error (HTTP $httpCode): $body",
        code = 2003,
        userMessage = "AI 老师接口异常 (${provider})，请稍后重试",
    )

    class NetworkTimeout(url: String) : TmasterException(
        message = "Network timeout: $url",
        code = 2004,
        userMessage = "网络连接超时，请检查网络后重试",
    )

    // ── 数据异常 ──────────────────────────────────────────────

    class SgfParseError(detail: String) : TmasterException(
        message = "SGF parse error: $detail",
        code = 3001,
        userMessage = "棋谱文件格式错误，请确认文件完整",
    )

    class GameDataCorrupted(gameId: String) : TmasterException(
        message = "Game data corrupted: $gameId",
        code = 3002,
        userMessage = "棋谱数据损坏，已跳过",
    )

    class DatabaseError(cause: Throwable) : TmasterException(
        message = "Database error: ${cause.message}",
        code = 3003,
        userMessage = "数据存储异常，请重启应用",
    )

    // ── 配置异常 ──────────────────────────────────────────────

    class InvalidConfig(key: String, detail: String) : TmasterException(
        message = "Invalid config '$key': $detail",
        code = 4001,
        userMessage = "配置项 '$key' 无效，已恢复默认值",
    )

    class ModelDownloadFailed(url: String, cause: Throwable?) : TmasterException(
        message = "Model download failed: $url",
        code = 4002,
        userMessage = "模型下载失败，请检查网络和存储空间",
    )

    class InsufficientStorage(requiredMb: Long, availableMb: Long) : TmasterException(
        message = "Storage insufficient: need ${requiredMb}MB, have ${availableMb}MB",
        code = 4003,
        userMessage = "存储空间不足（需要 ${requiredMb}MB，剩余 ${availableMb}MB）",
    )
}
