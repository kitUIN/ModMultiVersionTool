package io.github.kituin.modmultiversiontool

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
                lineCtx.firstIfContent = lineContent
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

    private fun checkTargetOneWay(targetFile: File): Boolean {
        targetFile.bufferedReader().use { reader ->
            var line: String?
            if (reader.readLine().also { line = it } != null) {
                if (hasKey(line!!.trimStart(), Keys.ONEWAY, true)) return true
            }
        }
        return false
    }

    public fun copy(
        sourceFile: File,
        targetFilePath: Path,
        folderName: String,
        loader: String,
        forward: Boolean = true
    ) {
        val lines = sourceFile.readLines()
        val map = createMap(folderName, if (forward) targetFilePath else sourceFile.toPath(), loader)
        val targetFile = targetFilePath.toFile()
        // 反向时检测是否是ONEWAY
        if (!forward && checkTargetOneWay(targetFile)) return
        val lineCtx = LineCtx(targetFile, map, forward)
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
        checkDirectory(lineCtx)
        lineCtx.targetFile.writeText(lineCtx.newLines.joinToString("\n"))
    }

    private fun checkDirectory(lineCtx: LineCtx) {
        if (!lineCtx.targetFile.exists()) {
            lineCtx.targetFile.parentFile.mkdirs()
            lineCtx.targetFile.createNewFile()
        }
    }


}