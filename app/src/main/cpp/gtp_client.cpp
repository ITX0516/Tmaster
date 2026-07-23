#include "gtp_client.h"
#include <sstream>
#include <iostream>
#include <android/log.h>

#define LOG_TAG "KataGo-GTP"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

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

    // TODO: 调用 KataGo C++ API 初始化引擎。
    // KataGo 的 setupDefaultConfig() 和 mainInitialization() 等函数
    // 需要在编译时链接 KataGo 的 libkatago.a。
    //
    // 实际集成步骤：
    // 1. 从 https://github.com/lightvector/KataGo clone 源码
    // 2. 修改 CMakeLists.txt 编译为 libkatago.a (ARM64)
    // 3. 调用 KataGo 的 public API：
    //
    //    ConfigParser cfg(configPath);
    //    SearchParams params = Setup::setupParams(cfg);
    //    NNEvaluator* nnEval = Setup::initializeNNEvaluator(modelPath, cfg, ...);
    //    Search* search = new Search(params, nnEval, ...);
    //
    // 当前版本使用 stdin/stdout 重定向的简化方案启动 KataGo GTP 主循环。

    running_ = true;
    ready_ = true;

    // 启动引擎线程
    engineThread_ = std::thread(&GtpClient::engineThread, this);

    // 等待引擎就绪（发送 boardsize 确认）
    sendAndWait("boardsize " + std::to_string(boardSize));
    sendAndWait("clear_board");

    LOGI("KataGo engine ready");
    return true;
}

void GtpClient::engineThread() {
    LOGI("Engine thread starting...");

    // ─────────────────────────────────────────────────────────
    // 这里是 KataGo GTP 主循环的入口。
    //
    // 当链接真实 KataGo 库后，调用方式：
    //
    //   // 重定向 stdin/stdout
    //   std::streambuf* oldCin = std::cin.rdbuf(inputStream.rdbuf());
    //   std::streambuf* oldCout = std::cout.rdbuf(outputStream.rdbuf());
    //   // 运行 KataGo main
    //   kataGoMain(argc, argv);
    //   // 恢复
    //   std::cin.rdbuf(oldCin);
    //   std::cout.rdbuf(oldCout);
    //
    // 当前版本：运行简单的 Mock GTP 循环用于开发调试。
    // ─────────────────────────────────────────────────────────

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

        // Mock GTP 处理
        std::string response;

        if (cmd.find("version") != std::string::npos) {
            response = "= 1.0\n\n";
        } else if (cmd.find("name") != std::string::npos) {
            response = "= KataGo (Tmaster)\n\n";
        } else if (cmd.find("boardsize") != std::string::npos) {
            response = "= \n\n";
        } else if (cmd.find("clear_board") != std::string::npos) {
            response = "= \n\n";
        } else if (cmd.find("komi") != std::string::npos) {
            response = "= \n\n";
        } else if (cmd.find("play") != std::string::npos) {
            response = "= \n\n";
        } else if (cmd.find("genmove") != std::string::npos) {
            response = "= pd\n\n";  // Mock: always play pd
        } else if (cmd.find("kata-analyze") != std::string::npos) {
            // Parse interval from command
            response = "= \n\n";
            // Analysis will be handled by analysisThread
        } else if (cmd.find("kata-set-rules") != std::string::npos) {
            response = "= \n\n";
        } else if (cmd.find("kata-stop") != std::string::npos) {
            response = "= \n\n";
        } else {
            response = "? unknown command\n\n";
        }

        // 返回响应
        {
            std::lock_guard<std::mutex> lock(mutex_);
            outputBuffer_ = response;
            responseReady_ = true;
        }
        responseCv_.notify_one();
    }

    LOGI("Engine thread stopped");
}

std::string GtpClient::sendCommand(const std::string& cmd) {
    LOGD("GTP → %s", cmd.c_str());
    std::string response = sendAndWait(cmd);
    LOGD("GTP ← %s", response.c_str());
    return response;
}

std::string GtpClient::sendAndWait(const std::string& cmd) {
    {
        std::lock_guard<std::mutex> lock(mutex_);
        inputBuffer_ = cmd;
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
    sendAndWait("kata-analyze interval 100");

    analyzing_ = true;
    analysisThread_ = std::thread(&GtpClient::analysisThread, this);
    LOGI("Analysis started");
}

void GtpClient::analysisThread() {
    while (analyzing_ && running_) {
        // 轮询分析结果 — KataGo 通过 GTP 异步返回 JSON
        // 实际集成时，KataGo 的分析引擎通过回调输出中间结果

        // Mock: 生成模拟分析 JSON 用于 UI 开发
        std::string mockJson = R"({
            "id": "katago-1",
            "isDuringSearch": true,
            "rootInfo": {"winrate": 0.52, "scoreLead": 0.5, "visits": 500},
            "moveInfos": [
                {"move": "pd", "visits": 200, "winrate": 0.52, "scoreLead": 0.5, "order": 0, "pv": ["pd", "dp"]},
                {"move": "dp", "visits": 150, "winrate": 0.48, "scoreLead": -0.3, "order": 1, "pv": ["dp", "pd"]},
                {"move": "pp", "visits": 80, "winrate": 0.45, "scoreLead": -1.2, "order": 2, "pv": ["pp", "dd"]}
            ]
        })";

        if (analysisCallback_) {
            analysisCallback_(mockJson);
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(500));
    }
}

void GtpClient::stopAnalysis() {
    analyzing_ = false;
    sendAndWait("kata-stop");
    if (analysisThread_.joinable()) {
        analysisThread_.join();
    }
    LOGI("Analysis stopped");
}

void GtpClient::destroy() {
    running_ = false;
    analyzing_ = false;
    responseCv_.notify_all();

    if (engineThread_.joinable()) {
        engineThread_.join();
    }
    if (analysisThread_.joinable()) {
        analysisThread_.join();
    }

    // 调用 KataGo 清理
    // delete search;
    // delete nnEval;

    ready_ = false;
    LOGI("Engine destroyed");
}
