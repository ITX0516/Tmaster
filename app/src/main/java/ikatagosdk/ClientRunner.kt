package ikatagosdk

/**
 * Client + Runner 的组合管理器。
 */
class ClientRunner {
    private var nativePtr: Long = 0

    init {
        _NewClientRunnerFromArgs(emptyArray())
    }

    private external fun _NewClientRunnerFromArgs(args: Array<String>)
    external fun getClient(): Client
    external fun setClient(client: Client)
    external fun getRunner(): KatagoRunner
    external fun setRunner(runner: KatagoRunner)
}
