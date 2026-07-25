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
            Log.i(TAG, "Step 1: Loading libkatago.so...")
            try {
                System.loadLibrary("katago")
                Log.i(TAG, "libkatago.so loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "libkatago.so loading failed (may be auto-loaded by gojni): ${e.message}")
            }

            Log.i(TAG, "Step 2: Loading libgojni.so...")
            System.loadLibrary("gojni")
            Log.i(TAG, "libgojni.so loaded successfully")

            loaded = true
            Log.i(TAG, "All native libraries loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load native library: ${e.message}", e)
            loadError = e
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
