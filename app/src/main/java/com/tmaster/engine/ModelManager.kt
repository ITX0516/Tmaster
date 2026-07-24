package com.tmaster.engine

import android.content.Context
import com.tmaster.error.TmasterException
import com.tmaster.log.ModuleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream

/**
 * 模型管理器 — 解压 assets/katago_weights/ 中的量化权重到内部存储。
 *
 * AhQ Go Lite 的模型是分割的量化文本权重 (.txt)，
 * 以 .gz 压缩存储在 APK 中。引擎通过 setKataWeight() 接收目录路径。
 */
class ModelManager(private val context: Context) {
    private val logger = ModuleLogger("Model")

    private val weightDir = File(context.filesDir, "katago/weights").also { it.mkdirs() }

    data class ModelInfo(
        val id: String,
        val name: String,
    )

    private val _currentModel = MutableStateFlow<ModelInfo?>(null)
    val currentModel: StateFlow<ModelInfo?> = _currentModel
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * 解压内置权重文件并返回权重目录路径。
     * 首次调用时解压，后续直接返回已解压路径。
     */
    suspend fun getDefaultModel(): String = withContext(Dispatchers.IO) {
        // 列出 assets/katago_weights/ 中的所有 .gz 文件
        val assetFiles = context.assets.list("katago_weights")
            ?.filter { it.endsWith(".gz") }
            ?: emptyList()

        logger.i("found ${assetFiles.size} weight files in assets")

        if (assetFiles.isEmpty()) {
            _error.value = "assets/katago_weights/ 中没有模型文件"
            throw TmasterException.ModelNotLoaded("assets/katago_weights/ is empty")
        }

        // 解压所有 .gz 文件
        for (filename in assetFiles) {
            val destFile = File(weightDir, filename.removeSuffix(".gz"))
            if (destFile.exists()) {
                logger.d("skip $filename (already extracted)")
                continue
            }

            try {
                context.assets.open("katago_weights/$filename").use { input ->
                    GZIPInputStream(input).use { gz ->
                        FileOutputStream(destFile).use { out ->
                            gz.copyTo(out)
                        }
                    }
                }
                logger.d("extracted $filename → ${destFile.name} (${destFile.length() / 1024}KB)")
            } catch (e: Exception) {
                logger.e("failed to extract $filename: ${e.message}")
                _error.value = "解压模型失败: $filename"
                throw TmasterException.ModelNotLoaded("extract $filename failed: ${e.message}")
            }
        }

        _currentModel.value = ModelInfo("builtin", "内置量化模型")
        logger.i("model ready at ${weightDir.absolutePath}")
        weightDir.absolutePath
    }
}
