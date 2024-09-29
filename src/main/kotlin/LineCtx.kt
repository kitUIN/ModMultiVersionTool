package io.github.kituin.modmultiversiontool

import io.github.kituin.modmultiversiontool.LineHelper.Companion.interpret
import java.io.File

/**

 * @param ifList if语句列表
 * @param inBlock 当前块是否允许转换
 * @param used 是否已经使用过if
 * @param inIfBlock 当前是否在if块中
 */
class IfInLine(
    var ifList: MutableList<String> = mutableListOf(),
    var inBlock: Boolean = false,
    var used: Boolean = false,
    var inIfBlock: Boolean = false,
)


/**
 *
 * @param targetFile 目标文件
 * @param map 上下文
 * @param forward 是否正向处理
 * @param topIfCheckList 当前处理行所处的树形结构中所有的if索引
 * @param ifInLineList 嵌套结构
 * @param oneWay 是否单向
 * @param header 是否是头部
 * @param index 当前处理的if体
 */
class LineCtx(
    var targetFile: File,
    val map: MutableMap<String, String>,
    val forward: Boolean,
    val topIfCheckList: MutableList<Int> = mutableListOf(),
    val ifInLineList: MutableList<IfInLine> = mutableListOf(),
    var oneWay: Boolean = false,
    var header: Boolean = false,
    var index: Int = -1,
) {
    fun clean() {
        if (!ifInLineList.isEmpty()) {
            ifInLineList.removeLast()
            index--
            topIfCheckList.removeLast()
        }
    }

    fun newIf(lineContent: String) {
        ifInLineList.add(IfInLine())
        index++
        last().inBlock = interpret(lineContent, Keys.IF, map) && checkTopIf()
        last().used = last().inBlock
        last().inIfBlock = true
        last().ifList.add(lineContent)
        topIfCheckList.add(0)
    }

    fun newElseIf(lineContent: String) {
        val inline = ifInLineList.last()
        last().inBlock = if (inline.inIfBlock) !inline.inBlock && beforeNotIf() && interpret(
            lineContent,
            Keys.ELSE_IF,
            map
        ) else inline.inBlock
        last().ifList.add(lineContent)
        if (last().inBlock) last().used = true
        topIfCheckList[index] += 1
    }
    fun newElse(lineContent: String) {
        last().inBlock = !last().used && !last().inBlock
        topIfCheckList[index] = -1
    }
    fun last(): IfInLine {
        return ifInLineList.last()
    }
    fun isEmpty(): Boolean {
        return ifInLineList.isEmpty()
    }
    /**
     * 当前处理行所处的树形结构中所有的if是否为true
     */
    private fun checkTopIf(): Boolean {
        for(ifIndex in 0 until index) {
            val ifListIndex = topIfCheckList[ifIndex]
            if(ifListIndex == -1) {
                for (i in ifInLineList[ifIndex].ifList) {
                    if (checkIf(i)) return false
                }
            }else{
                if (!checkIf(ifInLineList[ifIndex].ifList[ifListIndex])) return false
            }
        }
        return true
    }
    /**
     * 判断if是否true
     * @param i 判断行文字
     */
    private fun checkIf(i: String): Boolean {
        return if (i.startsWith(Keys.IF.value)) {
            interpret(i, Keys.IF, map)
        } else {
            interpret(i, Keys.ELSE_IF, map)
        }
    }

    /**
     * 判断是否是之前的if是不是全是false,全false才能使用else if
     */
    private fun beforeNotIf(): Boolean {
        if(!checkTopIf()) return false
        if (ifInLineList.isEmpty()) return false
        for (i in ifInLineList.last().ifList) {
            if (checkIf(i)) return false
        }
        return true
    }

}