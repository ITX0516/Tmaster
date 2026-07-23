package com.tmaster.engine

import com.tmaster.error.TmasterException
import com.tmaster.game.BoardState
import com.tmaster.game.Coord
import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.flow.Flow

/**
 * 远程引擎 — 通过 gRPC 连接云端 GPU（智星云、算云）或个人电脑。
 *
 * 支持平台:
 * - 智星云 (zhixingyun.com)
 * - 算云 (suanyun.com)
 * - 个人电脑 (自建 gRPC server)
 */
class RemoteEngine(
    override val id: String = "katago-remote",
    private val config: RemoteConfig,
) : GtpEngine {
    private val logger = ModuleLogger("Remote")

    override val name = "KataGo (Remote: ${config.host})"

    @Volatile
    override var isReady: Boolean = false
        private set

    override suspend fun initialize(boardSize: Int, komi: Double) {
        logger.i("connecting to remote engine at ${config.host}:${config.port}")
        isReady = true
        // TODO: gRPC handshake + auth with config.token
        throw TmasterException.RemoteConnectionFailed(
            "${config.host}:${config.port}", null,
        )
    }

    override fun analyze(state: BoardState): Flow<KataAnalysisResult> {
        // TODO: gRPC bidirectional stream
        throw UnsupportedOperationException("remote analyze not yet implemented")
    }

    override suspend fun generateMove(state: BoardState, temperature: Double): Coord {
        throw UnsupportedOperationException("remote generateMove not yet implemented")
    }

    override suspend fun playMove(state: BoardState) {}
    override suspend fun stopAnalysis() {}
    override suspend fun dispose() { isReady = false }
}

/**
 * 远程引擎连接配置。
 */
data class RemoteConfig(
    val host: String,           // IP 或域名
    val port: Int = 8080,
    val token: String? = null,  // 鉴权令牌
    val platform: RemotePlatform = RemotePlatform.PERSONAL,
    val useTls: Boolean = true,
)

enum class RemotePlatform {
    ZHIXINGYUN,   // 智星云
    SUANYUN,      // 算云
    PERSONAL,     // 个人电脑
}
