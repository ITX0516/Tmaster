package ikatagosdk

/**
 * KataGo SDK 入口 — 创建 Client 和 ClientRunner。
 */
object Ikatagosdk {
    init {
        System.loadLibrary("katago")
        System.loadLibrary("gojni")
    }

    external fun _init()
    external fun newClient(): Client
    external fun newClientRunnerFromArgs(args: Array<String>): ClientRunner
}
