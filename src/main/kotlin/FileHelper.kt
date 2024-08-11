package io.github.kituin.modmultiversiontool

import io.github.kituin.modmultiversioninterpreter.Interpreter
import io.github.kituin.modmultiversiontool.LineHelper.Companion.hasKey
import io.github.kituin.modmultiversiontool.LineHelper.Companion.interpret
import io.github.kituin.modmultiversiontool.LineHelper.Companion.isComment
import io.github.kituin.modmultiversiontool.LineHelper.Companion.replacement
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*

class FileHelper(
    val projectPath: String
) {
    /**
     * 创建一个包含特定键值对的映射。
     *
     * @param folderName 文件夹名称。
     * @param targetFilePath 目标文件路径。
     * @param loader 加载器名称。
     * @return 包含特定键值对的映射。
     */
    private fun createMap(
        folderName: String, targetFilePath: Path,
        loader: String
    ): MutableMap<String, String> {
        val folder = targetFilePath.parent.pathString
        val fileName = targetFilePath.name
        val fileNameWithoutExtension = targetFilePath.nameWithoutExtension
        return mutableMapOf(
            "$$" to folderName,
            "\$folder" to folder.removePrefix("$projectPath/"),
            "\$loader" to loader,
            "\$fileNameWithoutExtension" to fileNameWithoutExtension,
            "\$fileName" to fileName
        )
    }

    /**
     * 检查行内容是否在黑名单或白名单中，并根据特定规则进行处理。
     *
     * @param lineContent 行内容。
     * @param key 关键字，用于判断是黑名单还是白名单。
     * @param lineCtx 包含处理过程中所需信息的上下文对象。
     * @return 如果行内容在黑名单或白名单中，则返回true；否则返回false。
     */
    private fun blackOrWhiteList(lineContent: String, key: Keys, lineCtx: LineCtx): Boolean {
        if (lineCtx.forward && hasKey(lineContent, key)) {
            var delete = interpret(lineContent, key, lineCtx.map)
            if (key == Keys.ONLY) delete = !delete
            if (delete && lineCtx.targetFile.exists()) lineCtx.targetFile.delete()
            lineCtx.header = delete
            return true
        }
        return false
    }

    /**
     * 处理文件头部内容，并根据特定规则进行转换。
     *
     * @param lineContent 头部行内容。
     * @param lineCtx 包含处理过程中所需信息的上下文对象。
     */
    private fun processHeader(lineContent: String, lineCtx: LineCtx) {
        // 文件头部进行检测
        when {
            blackOrWhiteList(lineContent, Keys.EXCLUDE, lineCtx) || blackOrWhiteList(
                lineContent,
                Keys.ONLY,
                lineCtx
            ) -> {

            }

            lineCtx.forward && hasKey(lineContent, Keys.RENAME) -> {
                val rename = replacement(lineContent, Keys.RENAME, lineCtx.map)
                lineCtx.targetFile = File(lineCtx.map["\$folder"], rename)
            }

            hasKey(lineContent, Keys.ONEWAY) -> {
                if (!lineCtx.forward) lineCtx.header = true
                lineCtx.oneWay = true
            }
        }
    }

    /**
     * 处理每一行内容，并根据特定规则进行转换。
     *
     * @param prefix 注释前缀。
     * @param line 原始行内容。
     * @param trimmedLine 去除行首空白字符后的行内容。
     * @param lineContent 去除注释前缀后的行内容。
     * @param lineCtx 包含处理过程中所需信息的上下文对象。
     */
    private fun processLine(
        prefix: String, line: String, trimmedLine: String,
        lineContent: String, lineCtx: LineCtx
    ) {
        when {
            hasKey(lineContent, Keys.PRINT) && lineCtx.forward -> {
                lineCtx.newLines.add(
                    "$prefix ${
                        replacement(
                            lineContent,
                            Keys.RENAME,
                            lineCtx.map
                        )
                    }"
                )
                return
            }

            hasKey(lineContent, Keys.ELSE_IF) -> {
                lineCtx.inBlock = lineCtx.checkElseIf(lineContent)
                if (lineCtx.inBlock) lineCtx.used = true
            }

            hasKey(lineContent, Keys.IF) -> {
                lineCtx.inBlock = interpret(lineContent, Keys.IF, lineCtx.map)
                lineCtx.used = lineCtx.inBlock
                lineCtx.inIfBlock = true
                lineCtx.ifList.add(lineContent)
            }

            hasKey(lineContent, Keys.ELSE) && lineCtx.inIfBlock ->
                lineCtx.inBlock = !lineCtx.used && !lineCtx.inBlock

            hasKey(lineContent, Keys.END_IF) -> lineCtx.clean()
            lineCtx.inBlock -> {
                lineCtx.newLines.add(if (lineCtx.forward) trimmedLine.removePrefix(prefix) else "$prefix$line")
                return
            }
        }
        if (!trimmedLine.startsWith(prefix) || !lineCtx.oneWay) lineCtx.newLines.add(line)
    }
    /**
     * 检查目标文件是否为ONEWAY。
     *
     * @param targetFile 目标文件对象。
     * @return 如果目标文件是ONEWAY，则返回true；否则返回false。
     */
    private fun checkTargetOneWay(targetFile: File): Boolean {
        targetFile.bufferedReader().use { reader ->
            var line: String?
            if (reader.readLine().also { line = it } != null) {
                if (hasKey(line!!.trimStart(), Keys.ONEWAY, true)) return true
            }
        }
        return false
    }

    /**
     * 复制源文件到目标路径，并根据特定规则处理和转换内容。
     *
     * @param sourceFile 源文件对象。
     * @param targetFilePath 目标文件路径。
     * @param folderName 文件夹名称。
     * @param loader 加载器名称。
     * @param alias 别名替换。
     * @param forward 是否正向处理，默认为正向。
     */
    public fun copy(
        sourceFile: File,
        targetFilePath: Path,
        folderName: String,
        loader: String,
        alias: MutableMap<String, MutableMap<String, String>> = mutableMapOf(),
        forward: Boolean = true
    ) {
        val lines = sourceFile.readLines()
        val map = createMap(folderName, if (forward) targetFilePath else sourceFile.toPath(), loader)
        val targetFile = targetFilePath.toFile()
        // 反向时检测是否是ONEWAY
        if (!forward && checkTargetOneWay(targetFile)) return
        val lineCtx = LineCtx(targetFile, map, forward)
        extracted(lines, lineCtx)
        checkDirectory(lineCtx)
        lineCtx.targetFile.writeText(
            checkAlias(alias, lineCtx, forward,
                lineCtx.newLines.joinToString("\n")
            )
        )
    }
    /**
     * 检查并替换别名。
     *
     * @param alias 包含别名和对应值的映射。
     * @param lineCtx 包含处理过程中所需信息的上下文对象。
     * @param forward 是否正向处理。
     * @param res 需要替换的字符串。
     * @return 替换后的字符串。
     */
    private fun checkAlias(
        alias: MutableMap<String, MutableMap<String, String>> = mutableMapOf(),
        lineCtx: LineCtx,
        forward: Boolean,
        res: String
    ): String {
        var res1 = res
        for ((key, values) in alias) {
            for ((innerKey,innerValue) in values) {
                try {
                    if (Interpreter(innerKey, lineCtx.map).interpret()) {
                        res1 = if (forward) {
                            res1.replace(key, innerValue)
                        } else {
                            res1.replace(innerValue, key)
                        }
                        break
                    }
                } catch (_: Exception) {
                }
            }
        }
        return res1
    }

    /**
     * 处理给定的行列表，并根据特定规则提取和转换内容。
     *
     * @param lines 要处理的行列表。
     * @param lineCtx 包含处理过程中所需信息的上下文对象。
     */
    private fun extracted(
        lines: List<String>,
        lineCtx: LineCtx
    ) {
        var prefix: String? = null
        for (i in lines.indices) {
            val line = lines[i]
            val trimmedLine = line.trimStart()
            if (prefix == null) prefix = isComment(trimmedLine)
            prefix?.let {
                val lineContent = trimmedLine.removePrefix(it).trimStart()
                if (i <= 3) {
                    processHeader(lineContent, lineCtx)
                    if (lineCtx.header) return
                }
                processLine(prefix, line, trimmedLine, lineContent, lineCtx)
            } ?: lineCtx.newLines.add(line)
        }
    }

    /**
     * 检查目标文件目录是否存在，如果不存在则创建目录和文件。
     *
     * @param lineCtx 包含目标文件信息的上下文对象。
     */
    private fun checkDirectory(lineCtx: LineCtx) {
        if (!lineCtx.targetFile.exists()) {
            lineCtx.targetFile.parentFile.mkdirs()
            lineCtx.targetFile.createNewFile()
        }
    }


}