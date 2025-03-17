package io.github.kituin.modmultiversiontool

import java.nio.file.Path

interface IFileCopyWorker {
    fun copy(targetFilePath: Path, content: ByteArray)
    fun isSame(targetFilePath: Path, content: ByteArray): Boolean
}