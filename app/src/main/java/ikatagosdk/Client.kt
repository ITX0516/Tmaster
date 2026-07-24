package ikatagosdk

/**
 * KataGo 客户端 — 4 个 String 参数的构造方法 (来自反编译).
 * Client("", platform, username, password)
 */
class Client(
    empty: String,
    platform: String,
    username: String,
    password: String,
) {
    external fun createKatagoRunner(): KatagoRunner
    external fun setEngineType(type: String)
    external fun setGpuType(type: String)
    external fun setToken(token: String)
    external fun setExtraArgs(args: String)
    external fun setForceNode(node: String)
    external fun queryServer(): String

    companion object {
        init {
            System.loadLibrary("katago")
            System.loadLibrary("gojni")
        }
    }
}
