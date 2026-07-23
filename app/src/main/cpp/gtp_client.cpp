#include "gtp_client.h"
#include <sstream>
#include <iostream>
#include <android/log.h>

#define LOG_TAG "KataGo-GTP"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// ─────────────────────────────────────────────────────────────
// KATAGO_REAL 模式 — 链接 libkatago.a 后启用。
// 在 CMakeLists.txt 中取消 KATAGO_MOCK 改为 -DKATAGO_REAL 激活。
//
// 工作原理:
//   1. 重定向 std::cin / std::cout 到 stringstream
//   2. 在新线程调用 kataGoMain() → GTP 主循环
//   3. sendCommand() 写入输入流 → kataGoMain 读取
//   4. kataGoMain 的 cout 输出 → 线程捕获 → 返回给 JNI
//
// 参考: BadukAI / Ah Q Go 的 Android KataGo 集成方式
// ─────────────────────────────────────────────────────────────

GtpClient::GtpClient() = default;

GtpClient::~GtpClient() {
    destroy();
}

bool GtpClient::initialize(const std::string& modelPath,
                            const std::string& configPath,
                            int boardSize) {
    modelPath_ = modelPath;
    configPath_ = configPath;
    boardSize_ = boardSize;

    LOGI("Initializing KataGo: model=%s, config=%s, board=%d",
         modelPath.c_str(), configPath.c_str(), boardSize);

    running_ = true;

#ifdef KATAGO_REAL
    engineThread_ = std::thread(&GtpClient::engineThreadReal, this);
#else
    engineThread_ = std::thread(&GtpClient::engineThreadStub, this);
#endif

    // 发送基本设置
    sendAndWait("boardsize " + std::to_string(boardSize));
    sendAndWait("clear_board");

    ready_ = true;
    LOGI("KataGo ready");
    return true;
}

// ── 真实引擎线程 ───────────────────────────────────────────
#ifdef KATAGO_REAL

void GtpClient::engineThreadReal() {
    LOGI("KataGo GTP engine starting...");

    // 重定向 stdin/stdout → 内存流
    std::stringstream inputStream;
    std::stringstream outputStream;

    auto* oldCinBuf  = std::cin.rdbuf(inputStream.rdbuf());
    auto* oldCoutBuf = std::cout.rdbuf(outputStream.rdbuf());

    // 构造 KataGo 启动参数
    // kataGoMain 在 KataGo 源码中的 cpp/main.cpp 定义
    // 参数格式: katago gtp -model <path> -config <path>
    const char* argv[] = {
        "katago",
        "gtp",
        "-model", modelPath_.c_str(),
        "-config", configPath_.c_str(),
        nullptr
    };
    int argc = 6;

    // 在独立线程运行 KataGo GTP 主循环
    // KataGo 源码 cpp/main.cpp 中定义:
    extern int kataGoMain(int argc, const char* argv[]);
    kataGoMain(argc, argv);

    // 恢复标准 I/O
    std::cin.rdbuf(oldCinBuf);
    std::cout.rdbuf(oldCoutBuf);
    LOGI("KataGo GTP engine stopped");
}

#else

// ── 占位线程 (KataGo 未编译) ──────────────────────────────
void GtpClient::engineThreadStub() {
    LOGI("KataGo NOT compiled — engine is a stub.");
    LOGI("Run build_katago_android.sh then rebuild with -DKATAGO_REAL");

    while (running_) {
        std::string cmd;
        {
            std::unique_lock<std::mutex> lock(mutex_);
            responseCv_.wait(lock, [this] {
                return !inputBuffer_.empty() || !running_;
            });
            if (!running_) break;
            cmd = inputBuffer_;
            inputBuffer_.clear();
        }

        std::string response = "? KataGo not compiled. "
            "Run build_katago_android.sh first.\n\n";

        {
            std::lock_guard<std::mutex> lock(mutex_);
            outputBuffer_ = response;
            responseReady_ = true;
        }
        responseCv_.notify_one();
    }
    LOGI("Engine stub stopped");
}

#endif // KATAGO_REAL

// ── 通用方法 ───────────────────────────────────────────────

std::string GtpClient::sendCommand(const std::string& cmd) {
    LOGD("GTP → %s", cmd.c_str());
    std::string response = sendAndWait(cmd);
    LOGD("GTP ← %s", response.c_str());
    return response;
}

std::string GtpClient::sendAndWait(const std::string& cmd) {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        inputBuffer_ = cmd + "\n";
        responseReady_ = false;
    }
    responseCv_.notify_one();

    std::unique_lock<std::mutex> lock(mutex_);
    responseCv_.wait(lock, [this] { return responseReady_; });
    responseReady_ = false;
    return outputBuffer_;
}

void GtpClient::startAnalysis(AnalysisCallback callback) {
    if (analyzing_) return;
    analysisCallback_ = std::move(callback);

#ifdef KATAGO_REAL
    sendAndWait("kata-analyze interval 100");
#endif

    analyzing_ = true;
    analysisThread_ = std::thread(&GtpClient::analysisThread, this);
    LOGI("Analysis started");
}

void GtpClient::analysisThread() {
#ifdef KATAGO_REAL
    // 真实模式: 从 cout 流读取 KataGo 分析 JSON
    // GTP "kata-analyze" 会持续输出分析行到 stdout
    // 每行格式: {"id":"...","moveInfos":[...],"rootInfo":{...}}
    // 此线程读取这些行并回调
    while (analyzing_ && running_) {
        // TODO: 从 output stream 读取分析行
        // 需要重构流管理以区分 GTP 响应和分析输出
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
#else
    // 无 KataGo: 不产生分析数据
    while (analyzing_ && running_) {
        std::this_thread::sleep_for(std::chrono::milliseconds(500));
    }
#endif
}

void GtpClient::stopAnalysis() {
    analyzing_ = false;
#ifdef KATAGO_REAL
    sendAndWait("kata-stop");
#endif
    if (analysisThread_.joinable()) {
        analysisThread_.join();
    }
    LOGI("Analysis stopped");
}

void GtpClient::destroy() {
    running_ = false;
    analyzing_ = false;
    responseCv_.notify_all();

#ifdef KATAGO_REAL
    // 给 GTP 主循环发送 quit
    {
        std::lock_guard<std::mutex> lock(mutex_);
        inputBuffer_ = "quit\n";
    }
    responseCv_.notify_one();
#endif

    if (engineThread_.joinable()) engineThread_.join();
    if (analysisThread_.joinable()) analysisThread_.join();

    ready_ = false;
    LOGI("Engine destroyed");
}
