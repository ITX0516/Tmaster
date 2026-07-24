package ikatagosdk

/**
 * 引擎数据回调 — 接收分析结果。
 */
interface DataCallback {
    fun callback(data: String)
    fun onReady()
    fun stderrCallback(data: String)
}

/**
 * JNI 代理 — libgojni.so 通过此代理回调 Kotlin。
 */
internal class ProxyDataCallback(private val delegate: DataCallback) {
    external fun callback(data: String)
    external fun onReady()
    external fun stderrCallback(data: String)

    // 以下由 native 层回调
    private fun proxyCallback(data: String) = delegate.callback(data)
    private fun proxyOnReady() = delegate.onReady()
    private fun proxyStderrCallback(data: String) = delegate.stderrCallback(data)
}
