package ikatagosdk

import android.util.Log
import com.tmaster.log.FileLogger

object NativeLoader {
    private const val TAG = "NativeLoader"

    @Volatile
    private var loaded = false

    @Volatile
    var loadError: Throwable? = null
        private set

    @Synchronized
    fun ensureLoaded() {
        if (loaded) return
        loadError?.let { throw it }

        try {
            Log.i(TAG, "Loading libgojni.so...")
            System.loadLibrary("gojni")
            Log.i(TAG, "libgojni.so loaded successfully")
            loaded = true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load libgojni.so: ${e.message}", e)
            loadError = e
            try {
                FileLogger.e(TAG, "Failed to load libgojni.so: ${e.message}", e)
                FileLogger.flush()
            } catch (_: Exception) {}
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading libgojni.so: ${e.message}", e)
            loadError = e
            try {
                FileLogger.e(TAG, "Unexpected error loading libgojni.so: ${e.message}", e)
                FileLogger.flush()
            } catch (_: Exception) {}
            throw e
        }
    }

    fun isLoaded(): Boolean = loaded
}
