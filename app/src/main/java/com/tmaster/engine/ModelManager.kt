package com.tmaster.engine

import android.content.Context
import com.tmaster.error.TmasterException
import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 模型管理器 — 内置模型 + 可选联网升级。
 *
 * 策略:
 * - 内置 b18 (18 block) 模型在 assets/models/, 安装即用
 * - 可联网下载更大模型获得更强棋力
 * - 15b/8b 兼容旧版模型引用
 */
class ModelManager(private val context: Context) {
    private val logger = ModuleLogger("Model")

    private val modelDir = File(context.filesDir, "katago/models").also { it.mkdirs() }

    data class ModelInfo(
        val id: String,
        val name: String,
        val blocks: Int,
        val sizeMb: Int,
        val assetFile: String?,       // assets/models/ 中的文件名 (内置)
        val downloadUrl: String?,     // 联网下载 URL (可选)
    )

    val availableModels = listOf(
        ModelInfo("b18", "18 block (内置)", 18, 45,
            assetFile = "kata1-b18c384nbt.bin.gz",
            downloadUrl = null,
        ),
        ModelInfo("8b", "8 block (轻量)", 8, 8,
            assetFile = null,
            downloadUrl = "https://github.com/lightvector/KataGo/releases/download/v1.12.4/g170e-b8c384nbt-s7707771264-d4017878421.bin.gz",
        ),
        ModelInfo("15b", "15 block (标准)", 15, 30,
            assetFile = null,
            downloadUrl = "https://github.com/lightvector/KataGo/releases/download/v1.12.4/g170e-b15c192nbt-s1587755848-d3662226761.bin.gz",
        ),
    )

    private val _currentModel = MutableStateFlow<ModelInfo?>(null)
    val currentModel: StateFlow<ModelInfo?> = _currentModel

    /**
     * 获取模型文件路径 — 优先内置，其次已下载，最后才联网。
     * 内置模型在首次启动时从 assets 复制到内部存储。
     */
    suspend fun selectModel(modelId: String): String = withContext(Dispatchers.IO) {
        val model = availableModels.find { it.id == modelId }
            ?: throw TmasterException.InvalidConfig("model", "unknown model: $modelId")

        val file = File(modelDir, when {
            model.assetFile != null -> model.assetFile
            else -> "${model.id}.bin.gz"
        })

        // 1. 文件已存在 → 直接返回
        if (file.exists()) {
            logger.i("model ${model.name} already cached")
            _currentModel.value = model
            return@withContext file.absolutePath
        }

        // 2. 从 assets 复制 (内置模型)
        if (model.assetFile != null) {
            logger.i("extracting built-in model: ${model.name}")
            try {
                context.assets.open("models/${model.assetFile}").use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                logger.i("built-in model ready: ${model.name}")
                _currentModel.value = model
                return@withContext file.absolutePath
            } catch (e: Exception) {
                throw TmasterException.ModelNotLoaded(
                    "内置模型 ${model.assetFile} 未找到，APK 可能不完整",
                )
            }
        }

        // 3. 联网下载 (仅在用户主动选择大模型时)
        if (model.downloadUrl != null) {
            logger.i("downloading model ${model.name} (${model.sizeMb}MB)...")
            downloadModel(model, file)
            _currentModel.value = model
            return@withContext file.absolutePath
        }

        throw TmasterException.ModelNotLoaded("无法获取模型: ${model.name}")
    }

    /** 返回默认可用模型 (优先内置 b18)。 */
    suspend fun getDefaultModel(): String = selectModel("b18")

    private suspend fun downloadModel(model: ModelInfo, dest: File) {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        val request = okhttp3.Request.Builder().url(model.downloadUrl!!).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw TmasterException.ModelDownloadFailed(model.downloadUrl!!, null)
        }

        val body = response.body ?: throw TmasterException.ModelDownloadFailed(model.downloadUrl!!, null)
        body.byteStream().use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        logger.i("model ${model.name} downloaded (${dest.length() / 1024 / 1024}MB)")
    }
}
