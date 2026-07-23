cmake_minimum_required(VERSION 3.22.1)
project("katago-bridge")

# ── 源码 ───────────────────────────────────────────────────
add_library(
    katago-bridge
    SHARED
    katago_bridge.cpp
    gtp_client.cpp
)

find_library(log-lib log)
target_link_libraries(katago-bridge ${log-lib})

# ── 模式选择 ───────────────────────────────────────────────
# 目前: 未链接 KataGo，引擎返回 "KataGo 未编译" 错误。
#
# 要激活真实引擎:
#   1. 运行 scripts/build_katago_android.sh
#   2. 取消下面 4 行的注释
#   3. 把本行 add_definitions 改为 -DKATAGO_REAL

# add_library(katago STATIC IMPORTED)
# set_target_properties(katago PROPERTIES
#     IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/katago/libkatago.a)
# target_link_libraries(katago-bridge katago)
# add_definitions(-DKATAGO_REAL)

# C++17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2 -fexceptions -frtti")
