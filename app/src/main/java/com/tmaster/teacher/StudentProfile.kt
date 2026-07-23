package com.tmaster.teacher

/**
 * 学生画像 — 长期追踪学生棋力与弱点。
 *
 * 每次分析后自动更新。AI 老师根据画像提供个性化教学。
 */
data class StudentProfile(
    val id: String = "default",
    val name: String = "棋手",
    val level: String = "unknown",       // e.g. "18k", "3d", "9d"
    val skillScores: Map<String, Double> = mapOf(
        "fuseki" to 0.5,
        "life_and_death" to 0.5,
        "joseki" to 0.5,
        "chuban" to 0.5,
        "endgame" to 0.5,
        "shape" to 0.5,
    ),
    val weakPoints: List<WeakPoint> = emptyList(),
    val recentGames: Int = 0,
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
)

data class WeakPoint(
    val category: String,
    val frequency: Int,
    val lastSeen: Long = System.currentTimeMillis(),
)

/**
 * 学生画像仓库接口。
 */
interface StudentProfileStore {
    suspend fun read(): StudentProfile
    suspend fun write(profile: StudentProfile)
    suspend fun updateWeakPoints(problems: List<com.tmaster.analysis.ProblemMove>)
}
