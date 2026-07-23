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
 * GTP 客户端 — 在进程内管理 KataGo 引擎。
 *
 * 两种模式:
 * - 真实模式 (KATAGO_REAL): 链接 libkatago.a，运行 KataGo GTP 主循环
 * - 未就绪模式: 返回明确错误信息，提示需要编译 KataGo
 */
class GtpClient {
public:
    using AnalysisCallback = std::function<void(const std::string& json)>;

    GtpClient();
    ~GtpClient();

    bool initialize(const std::string& modelPath,
                    const std::string& configPath,
                    int boardSize);

    std::string sendCommand(const std::string& cmd);
    void startAnalysis(AnalysisCallback callback);
    void stopAnalysis();
    bool isReady() const { return ready_; }
    void destroy();

private:
#ifdef KATAGO_REAL
    void engineThreadReal();            // 真实 KataGo GTP 主循环
#else
    void engineThreadStub();            // 未就绪占位
#endif
    void analysisThread();
    std::string sendAndWait(const std::string& cmd);

    std::string inputBuffer_;
    std::string outputBuffer_;
    std::mutex mutex_;
    std::condition_variable responseCv_;
    bool responseReady_ = false;

    std::thread engineThread_;
    std::thread analysisThread_;

    std::atomic<bool> ready_{false};
    std::atomic<bool> running_{false};
    std::atomic<bool> analyzing_{false};
    AnalysisCallback analysisCallback_;

    std::string modelPath_;
    std::string configPath_;
    int boardSize_ = 19;
};

#endif
