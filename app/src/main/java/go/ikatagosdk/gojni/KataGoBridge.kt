package go.ikatagosdk.gojni

/**
 * KataGo JNI 桥接 — 使用 Ah Q Go 的预编译 libgojni.so。
 * 包名必须匹配 .so 中的 JNI 函数签名前缀。
 */
object KataGoBridge {
    init {
        System.loadLibrary("c++_shared")
        System.loadLibrary("gojni")
    }

    // 对外暴露的简单接口 — 内部调用 native 方法
    external fun init(modelPath: String, configPath: String, boardSize: Int): Boolean
    external fun sendGtp(cmd: String): String
    external fun destroy()
}
