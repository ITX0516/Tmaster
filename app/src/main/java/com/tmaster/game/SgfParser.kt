package com.tmaster.game

import com.tmaster.error.TmasterException

/**
 * SGF FF[4] 解析器 — 返回 [SgfNode] 树。
 */
class SgfParser(private val input: String) {
    private var pos = 0

    fun parse(): SgfNode {
        skipWhitespace()
        val root = parseCollection().firstOrNull()
            ?: throw TmasterException.SgfParseError("empty SGF")
        return root
    }

    private fun parseCollection(): List<SgfNode> {
        expect('(')
        val nodes = mutableListOf<SgfNode>()
        while (pos < input.length && input[pos] != ')') {
            if (input[pos] == '(') {
                val sub = parseCollection().firstOrNull() ?: continue
                if (nodes.isNotEmpty()) {
                    nodes[nodes.lastIndex] = nodes.last().copy(
                        children = nodes.last().children + sub
                    )
                } else nodes.add(sub)
            } else {
                nodes.add(parseNode())
            }
        }
        expect(')')
        return nodes
    }

    private fun parseNode(): SgfNode {
        expect(';')
        val props = mutableMapOf<String, SgfProperty>()
        while (pos < input.length && input[pos].isUpperCase()) {
            val prop = parseProperty()
            props[prop.name] = prop
        }
        return SgfNode(properties = props)
    }

    private fun parseProperty(): SgfProperty {
        val name = readPropName()
        val values = mutableListOf<String>()
        while (pos < input.length && input[pos] == '[') values.add(readPropValue())
        return SgfProperty(name, values)
    }

    private fun readPropName(): String {
        val start = pos
        while (pos < input.length && input[pos].isUpperCase()) pos++
        return input.substring(start, pos)
    }

    private fun readPropValue(): String {
        expect('[')
        val sb = StringBuilder()
        while (pos < input.length && input[pos] != ']') {
            if (input[pos] == '\\') {
                pos++
                if (pos < input.length) sb.append(input[pos++])
            } else {
                sb.append(input[pos++])
            }
        }
        expect(']')
        return sb.toString()
    }

    private fun skipWhitespace() {
        while (pos < input.length && input[pos] in " \t\n\r") pos++
    }

    private fun expect(c: Char) {
        skipWhitespace()
        if (pos >= input.length || input[pos] != c)
            throw TmasterException.SgfParseError("expected '$c' at $pos")
        pos++
    }
}

data class SgfProperty(val name: String, val values: List<String>)

class SgfNode(
    val properties: Map<String, SgfProperty> = emptyMap(),
    val children: List<SgfNode> = emptyList(),
) {
    fun prop(name: String): String? = properties[name]?.values?.firstOrNull()

    fun copy(
        properties: Map<String, SgfProperty> = this.properties,
        children: List<SgfNode> = this.children,
    ) = SgfNode(properties, children)
}

/**
 * 将 SGF 树转换为 [BoardState] 的主线序列。
 */
fun SgfNode.toBoardState(): BoardState {
    val sz = prop("SZ")?.toIntOrNull() ?: 19
    val km = prop("KM")?.toDoubleOrNull() ?: 6.5
    var state = BoardState.empty(sz, km)
    var node = this
    while (true) {
        val moveProp = node.properties["B"] ?: node.properties["W"]
        if (moveProp != null) {
            val color = if ("B" in node.properties) StoneColor.BLACK else StoneColor.WHITE
            val coord = Coord.fromSgf(moveProp.values.first())
            state = state.play(coord) ?: state.pass()
        }
        node = node.children.firstOrNull() ?: break
    }
    return state
}
