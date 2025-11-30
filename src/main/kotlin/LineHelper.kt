package io.github.kituin.modmultiversiontool

import io.github.kituin.modmultiversioninterpreter.Interpreter


class LineHelper {
    companion object {
        @JvmStatic
        fun isComment(line: String): String? = when {
            line.startsWith("//") -> "//"
            line.startsWith("#") -> "#"
            else -> null
        }


        @JvmStatic
        fun hasKey(line: String, key: Keys, unknownComment: Boolean = false): Boolean {
            if (key == Keys.END_IF || key == Keys.ELSE || key == Keys.PRINT || key == Keys.ONEWAY) return line.startsWith(key.value)
            return line.startsWith(key.value + " ")
        }

        @JvmStatic
        fun replacement(line: String, key: Keys, replacementMap: Map<String, String>): String {
            var current = line.removePrefix(key.value).trimStart()
            replacementMap.forEach { (replaceKey, value) ->
                current = current.replace(replaceKey, value)
            }
            return current
        }

        @JvmStatic
        fun interpret(line: String, key: Keys, replacementMap: Map<String, String>): Boolean =
            Interpreter(line.removePrefix(key.value), replacementMap).interpret()

    }
}