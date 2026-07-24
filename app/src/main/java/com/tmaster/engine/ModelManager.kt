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

// 模型管理器: 解压内置权重文件到内部存储。
// 权重文件来自 AhQ Go Lite 的 res/*.gz, 存放在 assets/katago_weights/
class ModelManager(private val context: Context) {
    private val logger = ModuleLogger("Model")

    private val weightDir = File(context.filesDir, "katago/weights").also { it.mkdirs() }

    data class ModelInfo(val id: String, val name: String)

    private val _currentModel = MutableStateFlow<ModelInfo?>(null)
    val currentModel: StateFlow<ModelInfo?> = _currentModel

    // 内置权重文件清单, 硬编码不依赖 assets.list()
    private val builtinWeights = listOf(
        "42.gz", "5f.gz", "8z.gz",
        "Cu.gz", "Kp.gz", "eo.gz",
        "mW.gz", "pG.gz", "s4.gz",
    )

    // 解压内置权重并返回权重目录路径。
    suspend fun getDefaultModel(): String = withContext(Dispatchers.IO) {
        logger.i("extracting ${builtinWeights.size} built-in weight files...")

        var extracted = 0
        for (filename in builtinWeights) {
            val destFile = File(weightDir, filename.removeSuffix(".gz"))
            if (destFile.exists()) {
                extracted++
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
                extracted++
                logger.d("$filename → ${destFile.name} (${destFile.length() / 1024}KB)")
            } catch (e: Exception) {
                logger.e("extract $filename failed: ${e.message}")
                throw TmasterException.ModelNotLoaded("解压 $filename 失败: ${e.message}")
            }
        }

        logger.i("extracted $extracted/${builtinWeights.size} weight files to $weightDir")
        if (extracted == 0) {
            // 文件都已存在但是没有解压？检查目录
            val files = weightDir.list()?.joinToString() ?: "(empty)"
            throw TmasterException.ModelNotLoaded("权重目录为空: $files (raw dir: $weightDir)")
        }

        _currentModel.value = ModelInfo("builtin", "内置量化模型")
        weightDir.absolutePath
    }
}
