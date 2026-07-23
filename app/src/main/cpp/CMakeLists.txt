cmake_minimum_required(VERSION 3.22.1)
project("katago-bridge")

# ── KataGo 集成说明 ─────────────────────────────────────────
#
# 要编译此项目，需要先编译 KataGo 为静态库。
#
# 步骤:
# 1. git clone https://github.com/lightvector/KataGo
# 2. 将 KataGo 源码放入 app/src/main/cpp/katago/
# 3. 编译 KataGo (Eigen 后端，无 GPU 依赖):
#
#    cd katago/cpp
#    cmake . -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
#            -DANDROID_ABI=arm64-v8a \
#            -DANDROID_PLATFORM=android-26 \
#            -DUSE_BACKEND=EIGEN \
#            -DBUILD_TESTING=OFF
#    make -j4
#
#    这会生成 libkatago.a
#
# 4. 取消下面 add_library 和 target_link_libraries 的注释
# ─────────────────────────────────────────────────────────────

add_library(
    katago-bridge
    SHARED
    katago_bridge.cpp
    gtp_client.cpp
)

# 链接 NDK 日志库
find_library(log-lib log)
target_link_libraries(katago-bridge ${log-lib})

# ── 链接 KataGo 静态库 (编译 KataGo 后取消注释) ────────────
# add_library(katago STATIC IMPORTED)
# set_target_properties(katago PROPERTIES
#     IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/katago/cpp/libkatago.a)
# target_link_libraries(katago-bridge katago)

# 预编译宏 — Mock模式 vs 真实引擎
# 开发阶段使用 MOCK 模式 (GTP client 返回模拟数据)
# 集成真实 KataGo 后改为 KATAGO_REAL
add_definitions(-DKATAGO_MOCK)

# C++17 (KataGo 要求)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 优化
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -fexceptions -frtti")
