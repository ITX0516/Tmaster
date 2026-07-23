cmake_minimum_required(VERSION 3.22.1)
project("katago-bridge")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -fexceptions -frtti")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DBYTE_ORDER=1234 -DLITTLE_ENDIAN=1234 -DBIG_ENDIAN=4321")

# ── 1. 我们的 JNI 桥接代码 ─────────────────────────────────
add_library(
    katago-bridge
    SHARED
    katago_bridge.cpp
    gtp_client.cpp
)

# ── 2. KataGo 源码 ─────────────────────────────────────────
# CI 编译后的 katago 二进制已复制到 katago/ 目录。
# 将 KataGo 源码加入编译，同 katago-bridge 一起链接为单 .so。
#
# 如果 katago/ 目录存在 KataGo 源文件，整体编译；
# 否则仅编译我们的 JNI 桥 (KATAGO_REAL 未定义时返回错误)。

file(GLOB KATAGO_SOURCES
    katago/cpp/core/*.cpp
    katago/cpp/game/*.cpp
    katago/cpp/neuralnet/*.cpp
    katago/cpp/search/*.cpp
    katago/cpp/command/*.cpp
    katago/cpp/dataio/*.cpp
    katago/cpp/program/*.cpp
    katago/cpp/tests/*.cpp
)
list(FILTER KATAGO_SOURCES EXCLUDE REGEX ".*_test\\.cpp$")

if(KATAGO_SOURCES)
    message(STATUS "KataGo sources found: ${KATAGO_SOURCES}")
    target_sources(katago-bridge PRIVATE ${KATAGO_SOURCES})
    target_include_directories(katago-bridge PRIVATE katago/cpp)
    target_compile_definitions(katago-bridge PRIVATE KATAGO_REAL)

    # Eigen3
    find_package(Eigen3 QUIET)
    if(Eigen3_FOUND)
        target_include_directories(katago-bridge PRIVATE ${EIGEN3_INCLUDE_DIR})
        target_compile_definitions(katago-bridge PRIVATE USE_EIGEN)
    endif()
else()
    message(WARNING "KataGo sources not found — engine will report error.")
    message(WARNING "Run scripts/build_katago_android.sh to clone KataGo")
endif()

find_library(log-lib log)
target_link_libraries(katago-bridge ${log-lib})
