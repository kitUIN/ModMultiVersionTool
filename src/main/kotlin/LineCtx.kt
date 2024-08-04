package io.github.kituin.modmultiversiontool

import io.github.kituin.modmultiversiontool.LineHelper.Companion.interpret
import java.io.File

class LineCtx(
    var targetFile: File,
    val map: MutableMap<String, String>,
    val forward: Boolean,
    val newLines: MutableList<String> = mutableListOf(),
    var firstIfContent: String = "",
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
    }

    private fun firstIf(): Boolean {
        if (firstIfContent != "") {
            return interpret(firstIfContent, Keys.IF, map)
        }
        return true
    }

    fun checkElseIf(lineContent: String): Boolean {
        return if (inIfBlock) !inBlock && !firstIf() && interpret(
            lineContent,
            Keys.ELSE_IF,
            map
        ) else inBlock
    }
}