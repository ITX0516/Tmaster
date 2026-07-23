/**
 * KataGo JNI 桥接层 — 连接 Kotlin 与 C++ 引擎。
 *
 * JNI 方法签名对应 com.tmaster.engine.LocalKataGo 中的 native 声明。
 */
#include <jni.h>
#include <string>
#include <android/log.h>
#include "gtp_client.h"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "KataGoJNI", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "KataGoJNI", __VA_ARGS__)

static GtpClient* g_engine = nullptr;
static std::string g_lastAnalysisJson;
static std::mutex g_analysisMutex;

// 分析回调 — KataGo 分析结果到达时调用
static void onAnalysisResult(const std::string& json) {
    std::lock_guard<std::mutex> lock(g_analysisMutex);
    g_lastAnalysisJson = json;
}

extern "C" {

// ── nativeInit ─────────────────────────────────────────────────
JNIEXPORT jboolean JNICALL
Java_com_tmaster_engine_LocalKataGo_nativeInit(
    JNIEnv* env, jobject /* this */,
    jstring modelPath, jstring configPath, jint boardSize) {

    const char* model = env->GetStringUTFChars(modelPath, nullptr);
    const char* config = env->GetStringUTFChars(configPath, nullptr);

    LOGI("nativeInit: model=%s, config=%s, board=%d", model, config, boardSize);

    if (g_engine != nullptr) {
        delete g_engine;
    }

    g_engine = new GtpClient();
    bool ok = g_engine->initialize(std::string(model),
                                   std::string(config),
                                   static_cast<int>(boardSize));

    env->ReleaseStringUTFChars(modelPath, model);
    env->ReleaseStringUTFChars(configPath, config);

    return ok ? JNI_TRUE : JNI_FALSE;
}

// ── nativeSend ─────────────────────────────────────────────────
JNIEXPORT jstring JNICALL
Java_com_tmaster_engine_LocalKataGo_nativeSend(
    JNIEnv* env, jobject /* this */, jstring cmd) {

    const char* cmdStr = env->GetStringUTFChars(cmd, nullptr);
    std::string response;

    if (g_engine != nullptr && g_engine->isReady()) {
        response = g_engine->sendCommand(std::string(cmdStr));
    } else {
        response = "? engine not initialized\n\n";
        LOGE("nativeSend called before engine initialization");
    }

    env->ReleaseStringUTFChars(cmd, cmdStr);
    return env->NewStringUTF(response.c_str());
}

// ── nativeAnalyzeStart ─────────────────────────────────────────
JNIEXPORT jboolean JNICALL
Java_com_tmaster_engine_LocalKataGo_nativeAnalyzeStart(
    JNIEnv* env, jobject /* this */, jint boardSize) {

    LOGI("nativeAnalyzeStart: board=%d", boardSize);

    if (g_engine == nullptr || !g_engine->isReady()) {
        LOGE("Cannot start analysis: engine not ready");
        return JNI_FALSE;
    }

    g_engine->startAnalysis(onAnalysisResult);
    return JNI_TRUE;
}

// ── nativeAnalyzePoll ──────────────────────────────────────────
JNIEXPORT jstring JNICALL
Java_com_tmaster_engine_LocalKataGo_nativeAnalyzePoll(
    JNIEnv* env, jobject /* this */) {

    std::string json;
    {
        std::lock_guard<std::mutex> lock(g_analysisMutex);
        json = g_lastAnalysisJson;
        g_lastAnalysisJson.clear();
    }
    return env->NewStringUTF(json.c_str());
}

// ── nativeAnalyzeStop ──────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_tmaster_engine_LocalKataGo_nativeAnalyzeStop(
    JNIEnv* /* env */, jobject /* this */) {

    LOGI("nativeAnalyzeStop");
    if (g_engine != nullptr) {
        g_engine->stopAnalysis();
    }
}

// ── nativeDestroy ──────────────────────────────────────────────
JNIEXPORT void JNICALL
Java_com_tmaster_engine_LocalKataGo_nativeDestroy(
    JNIEnv* /* env */, jobject /* this */) {

    LOGI("nativeDestroy");
    if (g_engine != nullptr) {
        g_engine->destroy();
        delete g_engine;
        g_engine = nullptr;
    }
}

} // extern "C"
