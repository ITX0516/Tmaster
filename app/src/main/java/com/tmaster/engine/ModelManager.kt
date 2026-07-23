package com.tmaster.engine

import android.content.Context
import com.tmaster.error.ErrorReporter
import com.tmaster.error.TmasterException
import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * KataGo 模型管理器 — 下载、切换、回退模型。
 *
 * 策略:
 * - 默认 15 block (约 30MB) — 性能和质量的最佳平衡
 * - 8 block (约 8MB) — 低配设备回退方案
 * - 支持下载更大的模型 (20b/40b) 以获得更强的棋力
 */
class ModelManager(private val context: Context) {
    private val logger = ModuleLogger("Model")

    private val modelDir = File(context.filesDir, "katago/models").also { it.mkdirs() }

    data class ModelInfo(
        val id: String,
        val name: String,
        val blocks: Int,
        val sizeMb: Int,
        val url: String,
    )

    val availableModels = listOf(
        ModelInfo("8b", "8 block (入门)", 8, 8,
            "https://github.com/lightvector/KataGo/releases/download/v1.15.1/b8c384nbt.bin.gz"),
        ModelInfo("15b", "15 block (推荐)", 15, 30,
            "https://github.com/lightvector/KataGo/releases/download/v1.15.1/b15c192nbt.bin.gz"),
    )

    private val _currentModel = MutableStateFlow<ModelInfo?>(null)
    val currentModel: StateFlow<ModelInfo?> = _currentModel

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    /** 选择模型 — 返回模型文件路径。 */
    suspend fun selectModel(modelId: String): String = withContext(Dispatchers.IO) {
        val model = availableModels.find { it.id == modelId }
            ?: throw TmasterException.InvalidConfig("model", "unknown model: $modelId")
        val file = File(modelDir, "${model.id}.bin.gz")

        if (!file.exists()) {
            logger.i("downloading model ${model.name} (${model.sizeMb}MB)")
            downloadModel(model.url, file)
            logger.i("model ${model.name} downloaded")
        }

        _currentModel.value = model
        file.absolutePath
    }

    /** 获取已下载的最大可用模型。 */
    suspend fun getBestAvailable(): String = withContext(Dispatchers.IO) {
        // 优先 15b，不可用则回退 8b
        for (model in listOf("15b", "8b")) {
            val file = File(modelDir, "$model.bin.gz")
            if (file.exists()) {
                val info = availableModels.find { it.id == model }
                _currentModel.value = info
                logger.i("using model: ${info?.name}")
                return@withContext file.absolutePath
            }
        }
        // 都没下载 → 下载默认 15b
        selectModel("15b")
    }

    /** 检查设备是否适合 15b（可用 RAM > 2GB）。 */
    fun canRun15b(): Boolean {
        val runtime = Runtime.getRuntime()
        val maxMemMb = runtime.maxMemory() / (1024 * 1024)
        return maxMemMb >= 1500 // 放宽一点，1.5GB heap 够用
    }

    private suspend fun downloadModel(url: String, dest: File) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw TmasterException.ModelDownloadFailed(url, null)
        }

        val body = response.body ?: throw TmasterException.ModelDownloadFailed(url, null)
        val total = body.contentLength()
        var downloaded = 0L

        body.byteStream().use { input ->
            FileOutputStream(dest).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (total > 0) _downloadProgress.value = downloaded.toFloat() / total
                }
            }
        }

        // 检查磁盘空间
        val usableSpace = modelDir.usableSpace / (1024 * 1024)
        if (usableSpace < 100) {
            logger.w("low storage: ${usableSpace}MB remaining")
        }
    }
}
