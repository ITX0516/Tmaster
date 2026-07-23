package com.tmaster.teacher

/**
 * 围棋知识卡 — AI 老师的教学素材。
 *
 * 知识卡分类覆盖围棋的各个领域，LLM 可以从本地知识库检索 2-4 张
 * 最相关的卡片，嵌入到教学回答中。
 */
data class KnowledgeCard(
    val id: String,
    val title: String,
    val content: String,       // 完整教学内容 (markdown)
    val tags: List<String>,    // 标签: ["joseki", "3-3", "invasion"]
    val category: String,      // 分类: "joseki", "fuseki", "tsumego", etc.
    val level: KnowledgeLevel,
)

enum class KnowledgeLevel { BEGINNER, INTERMEDIATE, ADVANCED }

/**
 * 本地知识库接口。
 */
interface KnowledgeBase {
    suspend fun search(query: String, limit: Int = 4): List<KnowledgeCard>
    suspend fun searchByTags(tags: List<String>, limit: Int = 4): List<KnowledgeCard>
    suspend fun addCard(card: KnowledgeCard)
    suspend fun removeCard(id: String)
}

/**
 * 内置知识卡 — 作为初始素材。
 */
object BuiltinKnowledge {
    val cards = listOf(
        KnowledgeCard(
            id = "fuseki_direction",
            title = "布局方向基础",
            content = "布局阶段，棋子应面向宽阔方向。..." +
                "基本原则：金角银边草肚皮，占角→守角/挂角→拆边。",
            tags = listOf("fuseki", "direction", "opening"),
            category = "fuseki",
            level = KnowledgeLevel.BEGINNER,
        ),
        KnowledgeCard(
            id = "joseki_33",
            title = "三三进角定式",
            content = "对手点三三时，挡住哪边决定外势方向。..." +
                "核心原则：挡的方向应该让自己的外势朝向有价值的地方。",
            tags = listOf("joseki", "3-3", "invasion"),
            category = "joseki",
            level = KnowledgeLevel.INTERMEDIATE,
        ),
        KnowledgeCard(
            id = "life_death_basics",
            title = "死活基础：两眼活棋",
            content = "一块棋至少需要两只眼才是活棋。..." +
                "常见眼形：直四、方四、刀五、花六。",
            tags = listOf("life_and_death", "tsumego", "basics"),
            category = "life_and_death",
            level = KnowledgeLevel.BEGINNER,
        ),
        KnowledgeCard(
            id = "shape_good",
            title = "好形与坏形",
            content = "好形：效率高、有弹性。坏形：愚形、凝聚。..." +
                "空三角是典型的坏形，跳和飞是基本的整形手段。",
            tags = listOf("shape", "haengma"),
            category = "shape",
            level = KnowledgeLevel.INTERMEDIATE,
        ),
        KnowledgeCard(
            id = "sente_gote",
            title = "先手与后手",
            content = "先手 = 对方必须应的棋。后手 = 对方可以不应的棋。..." +
                "收官阶段，先手官子价值是后手的两倍。",
            tags = listOf("endgame", "sente", "gote"),
            category = "endgame",
            level = KnowledgeLevel.BEGINNER,
        ),
    )
}
