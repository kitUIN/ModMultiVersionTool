package io.github.kituin.modmultiversiontool

import io.github.kituin.modmultiversiontool.LineHelper.Companion.interpret
import java.io.File

class LineCtx(
    var targetFile: File,
    val map: MutableMap<String, String>,
    val forward: Boolean,
    val newLines: MutableList<String> = mutableListOf(),
    var ifList: MutableList<String> = mutableListOf(),
    var inBlock: Boolean = false,
    var used: Boolean = false,
    var inIfBlock: Boolean = false,
    var oneWay: Boolean = false,
    var header: Boolean = false
) {
    fun clean() {
        this.inBlock = false
        this.inIfBlock = false
        this.used = false
        this.ifList.clear()
    }

    private fun firstIf(): Boolean {
        for (i in ifList) {
            if (i.startsWith(Keys.IF.value)) {
                if (!interpret(i, Keys.IF, map)) return false
            } else {
                if (!interpret(i, Keys.ELSE_IF, map)) return false
            }
        }
        return true
    }

    fun checkElseIf(lineContent: String): Boolean {
        val res = if (inIfBlock) !inBlock && !firstIf() && interpret(
            lineContent,
            Keys.ELSE_IF,
            map
        ) else inBlock
        ifList.add(lineContent)
        return res
    }
}