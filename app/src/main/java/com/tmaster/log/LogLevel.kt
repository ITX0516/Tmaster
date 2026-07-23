package com.tmaster.log

enum class LogLevel(val priority: Int) {
    VERBOSE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4);

    companion object {
        fun fromPriority(p: Int): LogLevel = when (p) {
            0 -> VERBOSE; 1 -> DEBUG; 2 -> INFO; 3 -> WARN; 4 -> ERROR
            else -> ERROR
        }
    }
}
