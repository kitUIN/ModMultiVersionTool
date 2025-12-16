import io.github.kituin.modmultiversiontool.CommentMode
import io.github.kituin.modmultiversiontool.FileHelper
import io.github.kituin.modmultiversiontool.LineCtx
import io.github.kituin.modmultiversiontool.LocalFileCopyWorker
import java.io.File
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 针对注释模式的新功能测试
 */
class TestCommentMode {
    private val helper: FileHelper = FileHelper("", LocalFileCopyWorker())

    /**
     * 测试开启代码前注释
     */
    @Test
    fun testCommentBeforeCode1() {
        val lines = """
// 这行注释是用来预防注释符号识别失败
package io.github.kituin.chatimage.command;
public class ReloadConfig implements Command<#CommandSourceStack#>{
    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        ))
        // IF forge-1.16.5
        //CONFIG = ChatImageConfig.loadConfig();
        // END IF
        return Command.SINGLE_SUCCESS;
    }
}
        """.trimIndent().split("\n")
        val lineCtx = LineCtx(
            Path("").toFile(), mutableMapOf(
                "$$" to "forge-1.16.5",
                "\$folder" to "forge/forge-1.16.5",
                "\$loader" to "forge",
                "\$fileNameWithoutExtension" to "",
                "\$fileName" to ""
            ), forward = true, commentMode = CommentMode(beforeCode = true)
        )
        val newLines = helper.extracted(lines, lineCtx, File(""))
        assertEquals(newLines!!.joinToString("\n"), """
// 这行注释是用来预防注释符号识别失败
package io.github.kituin.chatimage.command;
public class ReloadConfig implements Command<#CommandSourceStack#>{
    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        ))
        // IF forge-1.16.5
        CONFIG = ChatImageConfig.loadConfig();
        // END IF
        return Command.SINGLE_SUCCESS;
    }
}
        """.trimIndent())
    }
    /**
     * 测试开启代码前注释(带一个空格)
     */
    @Test
    fun testCommentBeforeCode2() {
        val lines = """
// 这行注释是用来预防注释符号识别失败
package io.github.kituin.chatimage.command;
public class ReloadConfig implements Command<#CommandSourceStack#>{
    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        ))
        // IF forge-1.16.5
        // CONFIG = ChatImageConfig.loadConfig();
        // END IF
        return Command.SINGLE_SUCCESS;
    }
}
        """.trimIndent().split("\n")
        val lineCtx = LineCtx(
            Path("").toFile(), mutableMapOf(
                "$$" to "forge-1.16.5",
                "\$folder" to "forge/forge-1.16.5",
                "\$loader" to "forge",
                "\$fileNameWithoutExtension" to "",
                "\$fileName" to ""
            ), forward = true, commentMode = CommentMode(beforeCode = true, withOneSpace = true)
        )
        val newLines = helper.extracted(lines, lineCtx, File(""))
        assertEquals(newLines!!.joinToString("\n"), """
// 这行注释是用来预防注释符号识别失败
package io.github.kituin.chatimage.command;
public class ReloadConfig implements Command<#CommandSourceStack#>{
    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        ))
        // IF forge-1.16.5
        CONFIG = ChatImageConfig.loadConfig();
        // END IF
        return Command.SINGLE_SUCCESS;
    }
}
        """.trimIndent())
    }
    /**
     * 反向测试开启代码前注释
     */
    @Test
    fun testBackCommentBeforeCode1() {
        val lines = """
// 这行注释是用来预防注释符号识别失败
package io.github.kituin.chatimage.command;
public class ReloadConfig implements Command<#CommandSourceStack#>{
    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        ))
        // IF forge-1.16.5
        CONFIG = ChatImageConfig.loadConfig();
        // END IF
        return Command.SINGLE_SUCCESS;
    }
}
        """.trimIndent().split("\n")
        val lineCtx = LineCtx(
            Path("").toFile(), mutableMapOf(
                "$$" to "forge-1.16.5",
                "\$folder" to "forge/forge-1.16.5",
                "\$loader" to "forge",
                "\$fileNameWithoutExtension" to "",
                "\$fileName" to ""
            ), forward = false, commentMode = CommentMode(beforeCode = true)
        )
        val newLines = helper.extracted(lines, lineCtx, File(""))
        assertEquals(newLines!!.joinToString("\n"), """
// 这行注释是用来预防注释符号识别失败
package io.github.kituin.chatimage.command;
public class ReloadConfig implements Command<#CommandSourceStack#>{
    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        ))
        // IF forge-1.16.5
        //CONFIG = ChatImageConfig.loadConfig();
        // END IF
        return Command.SINGLE_SUCCESS;
    }
}
        """.trimIndent())
    }
    /**
     * 反向测试开启代码前注释(带一个空格)
     */
    @Test
    fun testBackCommentBeforeCode2() {
        val lines = """
// 这行注释是用来预防注释符号识别失败
package io.github.kituin.chatimage.command;
public class ReloadConfig implements Command<#CommandSourceStack#>{
    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        ))
        // IF forge-1.16.5
        CONFIG = ChatImageConfig.loadConfig();
        // END IF
        return Command.SINGLE_SUCCESS;
    }
}
        """.trimIndent().split("\n")
        val lineCtx = LineCtx(
            Path("").toFile(), mutableMapOf(
                "$$" to "forge-1.16.5",
                "\$folder" to "forge/forge-1.16.5",
                "\$loader" to "forge",
                "\$fileNameWithoutExtension" to "",
                "\$fileName" to ""
            ), forward = false, commentMode = CommentMode(beforeCode = true, withOneSpace = true)
        )
        val newLines = helper.extracted(lines, lineCtx, File(""))
        assertEquals(newLines!!.joinToString("\n"), """
// 这行注释是用来预防注释符号识别失败
package io.github.kituin.chatimage.command;
public class ReloadConfig implements Command<#CommandSourceStack#>{
    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        ))
        // IF forge-1.16.5
        // CONFIG = ChatImageConfig.loadConfig();
        // END IF
        return Command.SINGLE_SUCCESS;
    }
}
        """.trimIndent())
    }
    @Test
    fun testOneWay() {
        val lines = """
// ONEWAY
// RENAME test.java
package io.github.kituin.chatimage.command;
public class ReloadConfig implements Command<#CommandSourceStack#>{
    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        ))
//      IF forge-1.16.5
//         CONFIG = ChatImageConfig.loadConfig();
//      END IF
        return Command.SINGLE_SUCCESS;
    }
}
        """.trimIndent().split("\n")
        val lineCtx = LineCtx(
            Path("").toFile(), mutableMapOf(
                "$$" to "forge-1.16.5",
                "\$folder" to "forge/forge-1.16.5",
                "\$loader" to "forge",
                "\$fileNameWithoutExtension" to "",
                "\$fileName" to ""
            ), forward = true, commentMode = CommentMode(beforeCode = true, withOneSpace = true)
        )
        val newLines = helper.extracted(lines, lineCtx, File(""))
        assertEquals(newLines!!.joinToString("\n"), """
package io.github.kituin.chatimage.command;
public class ReloadConfig implements Command<#CommandSourceStack#>{
    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        ))
        CONFIG = ChatImageConfig.loadConfig();
        return Command.SINGLE_SUCCESS;
    }
}
        """.trimIndent())
    }
}