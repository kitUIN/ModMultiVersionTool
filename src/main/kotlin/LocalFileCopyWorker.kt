package io.github.kituin.modmultiversiontool

import java.nio.file.Path

class LocalFileCopyWorker : IFileCopyWorker {
    override fun copy(targetFilePath: Path, content: ByteArray) {
        targetFilePath.toFile().writeBytes(content)
    }

    override fun isSame(targetFilePath: Path, content: ByteArray): Boolean {
        return targetFilePath.toFile().readBytes().contentEquals(content)
    }
}