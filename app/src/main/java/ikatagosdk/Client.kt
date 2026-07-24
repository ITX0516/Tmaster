package ikatagosdk

/**
 * KataGo 客户端 — 管理引擎连接（本地或远程）。
 */
class Client {
    private var nativePtr: Long = 0

    init {
        _NewClient()
    }

    private external fun _NewClient()
    external fun createKatagoRunner(): KatagoRunner
    external fun setEngineType(type: String)   // "local" | "remote"
    external fun setGpuType(type: String)       // "eigen" | "opencl" | "cuda"
    external fun setToken(token: String)
    external fun setExtraArgs(args: String)
    external fun setForceNode(node: String)
    external fun queryServer(host: String): String
}
