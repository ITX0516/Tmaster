package ikatagosdk

/**
 * KataGo 引擎运行器 — JNI 对接 libgojni.so。
 * 管理引擎进程、发送 GTP 命令、接收分析数据。
 */
class KatagoRunner {
    private var nativePtr: Long = 0

    init {
        _New()
    }

    // ── Native 方法 ────────────────────────────────────────
    private external fun _New()
    external fun run(): Boolean
    external fun runWithStdio(): Boolean
    external fun sendGTPCommand(cmd: String): String
    external fun setKataConfig(configPath: String)
    external fun setKataLocalConfig(key: String, value: String)
    external fun setKataName(name: String)
    external fun setKataWeight(weightPath: String, configPath: String)
    external fun setKataOverrideConfig(key: String, value: String)
    external fun setRefreshInterval(intervalMs: Int)
    external fun setSubCommands(commands: Array<String>)
    external fun setTransmitMoveNum(moveNum: Int)
    external fun setClientID(clientId: String)
    external fun setExtraInfo(key: String, value: String)
    external fun disableCompress()

    companion object {
        init {
            System.loadLibrary("katago")
            System.loadLibrary("gojni")
        }
    }
}
