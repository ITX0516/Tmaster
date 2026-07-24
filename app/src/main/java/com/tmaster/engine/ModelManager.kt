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

class ModelManager(private val context: Context) {
    private val logger = ModuleLogger("Model")
    private val weightDir = File(context.filesDir, "katago/weights").also { it.mkdirs() }

    data class ModelInfo(val id: String, val name: String)
    private val _currentModel = MutableStateFlow<ModelInfo?>(null)
    val currentModel: StateFlow<ModelInfo?> = _currentModel

    // res/raw/ 中的资源名 (Android要求以字母开头)
    private val resNames = listOf("w_42", "w_5f", "w_8z", "Cu", "Kp", "eo", "mW", "pG", "s4")

    suspend fun getDefaultModel(): String = withContext(Dispatchers.IO) {
        logger.i("extracting ${resNames.size} weight files from res/raw/")

        var extracted = 0
        for (name in resNames) {
            val destFile = File(weightDir, name)
            if (destFile.exists()) { extracted++; continue }

            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId == 0) {
                logger.e("resource R.raw.$name not found!")
                throw TmasterException.ModelNotLoaded("找不到内置权重: $name")
            }

            try {
                context.resources.openRawResource(resId).use { input ->
                    GZIPInputStream(input).use { gz ->
                        FileOutputStream(destFile).use { out -> gz.copyTo(out) }
                    }
                }
                extracted++
                logger.d("$name → ${destFile.length()/1024}KB")
            } catch (e: Exception) {
                logger.e("extract $name failed: ${e.message}")
                throw TmasterException.ModelNotLoaded("解压 $name 失败: ${e.message}")
            }
        }

        logger.i("extracted $extracted/${resNames.size} files to $weightDir")
        _currentModel.value = ModelInfo("builtin", "内置量化模型")
        weightDir.absolutePath
    }
}
