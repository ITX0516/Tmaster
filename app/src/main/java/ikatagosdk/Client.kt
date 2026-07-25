package ikatagosdk

/**
 * KataGo 客户端 — 由 Ikatagosdk.newClient() 工厂方法创建。
 * 不应直接构造（Go native 层没有构造函数 JNI 符号）。
 */
class Client {
    init {
        NativeLoader.ensureLoaded()
    }

    // Go cgo 编码: __NewClient → JNI 函数名 _1_1NewClient
    external fun __NewClient()
    external fun createKatagoRunner(): KatagoRunner
    external fun queryServer(): String
    external fun setEngineType(type: String)
    external fun setGpuType(type: String)
    external fun setToken(token: String)
    external fun setExtraArgs(args: String)
    external fun setForceNode(node: String)

    companion object
}
