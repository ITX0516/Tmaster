package ikatagosdk

import android.util.Log
import com.tmaster.log.FileLogger

/**
 * 统一管理 native 库加载，确保在任何 native 方法调用前库已加载。
 *
 * 为什么不用 companion object init？
 * Kotlin 的 companion object init 是惰性的，只有访问伴生对象成员时才会执行。
 * 单纯实例化类并调用实例方法不会触发伴生对象初始化，
 * 导致调用 external 方法时 native 库还没加载，报 "No implementation found"。
 */
object NativeLoader {
    private const val TAG = "NativeLoader"

    @Volatile
    private var loaded = false

    @Volatile
    var loadError: Throwable? = null
        private set

    /**
     * 确保 native 库已加载。线程安全，多次调用无害。
     */
    @Synchronized
    fun ensureLoaded() {
        if (loaded) return
        loadError?.let { throw it }

        Log.i(TAG, "Loading native library gojni...")
        try {
            System.loadLibrary("gojni")
            Log.i(TAG, "libgojni.so loaded successfully")
            loaded = true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library: ${e.message}", e)
            loadError = e
            // 同时写入文件日志，确保崩溃后能查到
            try {
                FileLogger.e(TAG, "Failed to load native library: ${e.message}", e)
                FileLogger.flush()
            } catch (_: Exception) {}
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading native library: ${e.message}", e)
            loadError = e
            try {
                FileLogger.e(TAG, "Unexpected error loading native library: ${e.message}", e)
                FileLogger.flush()
            } catch (_: Exception) {}
            throw e
        }
    }

    fun isLoaded(): Boolean = loaded
}
