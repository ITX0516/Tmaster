#ifndef GTP_CLIENT_H
#define GTP_CLIENT_H

#include <string>
#include <functional>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <queue>
#include <atomic>

/**
 * GTP 客户端 — 在进程内管理 KataGo GTP 引擎。
 *
 * 工作方式：将 KataGo 的 stdin/stdout 重定向到内存缓冲区，
 * 通过 sendCommand() 发送 GTP 命令，读取响应返回给 JNI 层。
 *
 * 参考：BadukAI 和 Ah Q Go 的实现方式。
 */
class GtpClient {
public:
    using AnalysisCallback = std::function<void(const std::string& json)>;

    GtpClient();
    ~GtpClient();

    /**
     * 初始化 KataGo 引擎。
     * @param modelPath  权重文件路径 (e.g. "b15c192nbt.bin.gz")
     * @param configPath 配置文件路径 (e.g. "katago.cfg")
     * @param boardSize  棋盘大小 (9/13/19)
     * @return 成功返回 true
     */
    bool initialize(const std::string& modelPath,
                    const std::string& configPath,
                    int boardSize);

    /** 发送一条 GTP 命令，等待并返回响应。线程安全。 */
    std::string sendCommand(const std::string& cmd);

    /**
     * 启动后台分析线程。
     * 结果通过 [callback] 异步返回，格式为 KataGo JSON。
     */
    void startAnalysis(AnalysisCallback callback);

    /** 停止分析线程。 */
    void stopAnalysis();

    /** 引擎是否就绪。 */
    bool isReady() const { return ready_; }

    /** 释放所有资源。 */
    void destroy();

private:
    void engineThread();           // KataGo GTP 主循环线程
    void analysisThread();         // 分析轮询线程
    std::string sendAndWait(const std::string& cmd);

    // 输入/输出缓冲区
    std::string inputBuffer_;
    std::string outputBuffer_;

    // 同步
    std::mutex mutex_;
    std::condition_variable responseCv_;
    bool responseReady_ = false;

    // 线程
    std::thread engineThread_;
    std::thread analysisThread_;

    // 状态
    std::atomic<bool> ready_{false};
    std::atomic<bool> running_{false};
    std::atomic<bool> analyzing_{false};
    AnalysisCallback analysisCallback_;

    // 路径
    std::string modelPath_;
    std::string configPath_;
    int boardSize_ = 19;
};

#endif // GTP_CLIENT_H
