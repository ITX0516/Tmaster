package ikatagosdk

class KatagoRunner {
    init {
        NativeLoader.ensureLoaded()
    }

    external fun sendGTPCommand(cmd: String): String
    external fun stop()
    external fun run(): Boolean
    external fun runWithStdio(): Boolean
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

    companion object
}
