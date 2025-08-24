package io.github.kituin.modmultiversiontool

import java.nio.file.Path
import kotlin.io.path.exists

class LocalFileCopyWorker : IFileCopyWorker {
    override fun copy(targetFilePath: Path, content: ByteArray) {
        targetFilePath.toFile().writeBytes(content)
    }

    override fun isSame(targetFilePath: Path, content: ByteArray): Boolean {
        return targetFilePath.exists() && targetFilePath.toFile().readBytes().contentEquals(content)
    }
}