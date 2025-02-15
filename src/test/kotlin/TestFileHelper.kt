import io.github.kituin.modmultiversiontool.CommentMode
import io.github.kituin.modmultiversiontool.FileHelper
import io.github.kituin.modmultiversiontool.LineCtx
import java.io.File
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TestFileHelper {

    private val helper: FileHelper = FileHelper("")

    /**
     * 测试嵌套if
     */
    @Test
    fun testMul() {
        val lines = """
// IF < forge-1.19
// IF >= forge-1.16.5
// 1
// ELSE IF > forge-1.10
// 3
// END IF
// 2
// END IF
        """.trimIndent().split("\n")
        val lineCtx = LineCtx(
            Path("").toFile(), mutableMapOf(
                "$$" to "forge-1.14.5",
                "\$folder" to "forge/forge-1.16.5",
                "\$loader" to "forge",
                "\$fileNameWithoutExtension" to "",
                "\$fileName" to ""
            ), forward = true
        )
        val newLines = helper.extracted(lines, lineCtx,File(""))
        assertEquals(newLines!!.joinToString("\n"), """
// IF < forge-1.19
// IF >= forge-1.16.5
// 1
// ELSE IF > forge-1.10
 3
// END IF
 2
// END IF
        """.trimIndent())
    }



    @Test
    fun testBug() {
        val lines = """
// 这个注释是用来预防注释符号识别失败
package io.github.kituin.chatimage.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.github.kituin.ChatImageCode.ChatImageConfig;
import net.minecraft.client.Minecraft;


import static io.github.kituin.chatimage.ChatImage.CONFIG;
import static io.github.kituin.chatimage.tool.SimpleUtil.createTranslatableComponent;

public class ReloadConfig implements Command<#CommandSourceStack#>{

    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        CONFIG = ChatImageConfig.loadConfig();
        context.getSource().#sendSystemMessage#(
                createTranslatableComponent("success.reload.chatimage.command")
                        .setStyle(#Style#.EMPTY.withColor(
                                #ChatFormatting#.GREEN
        ))
// IF forge-1.16.5
//       hellow
// END IF
            );
        return Command.SINGLE_SUCCESS;
    }
    public final static ReloadConfig instance = new ReloadConfig();
}
        """.trimIndent().split("\n")
        val lineCtx = LineCtx(
            Path("").toFile(), mutableMapOf(
                "$$" to "forge-1.16.5",
                "\$folder" to "forge/forge-1.16.5",
                "\$loader" to "forge",
                "\$fileNameWithoutExtension" to "",
                "\$fileName" to ""
            ), forward = true
        )
        val newLines = helper.extracted(lines, lineCtx,File(""))
        assertEquals(newLines!!.joinToString("\n"), """
// 这个注释是用来预防注释符号识别失败
package io.github.kituin.chatimage.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import io.github.kituin.ChatImageCode.ChatImageConfig;
import net.minecraft.client.Minecraft;


import static io.github.kituin.chatimage.ChatImage.CONFIG;
import static io.github.kituin.chatimage.tool.SimpleUtil.createTranslatableComponent;

public class ReloadConfig implements Command<#CommandSourceStack#>{

    @Override
    public int run(CommandContext<#CommandSourceStack#>context) {
        CONFIG = ChatImageConfig.loadConfig();
        context.getSource().#sendSystemMessage#(
                createTranslatableComponent("success.reload.chatimage.command")
                        .setStyle(#Style#.EMPTY.withColor(
                                #ChatFormatting#.GREEN
        ))
// IF forge-1.16.5
       hellow
// END IF
            );
        return Command.SINGLE_SUCCESS;
    }
    public final static ReloadConfig instance = new ReloadConfig();
}
        """.trimIndent())
    }

    @Test
    fun testMulBug() {
        val lines = """
// IF <= forge-1.19
//        renderBackground(matrixStack);
//        drawCenteredString(matrixStack, this.font,
//                title, this.width / 2, this.height / 4 - 16, 16764108);
// ELSE
// IF forge-1.20
//        renderBackground(matrixStack);
// ELSE
//         renderBackground(matrixStack, mouseX, mouseY, partialTicks);
// END IF
//        matrixStack.drawCenteredString(this.font,
//                title, this.width / 2, this.height / 4 - 16, 16764108);
// END IF
        """.trimIndent().split("\n")
        val lineCtx = LineCtx(
            Path("").toFile(), mutableMapOf(
                "$$" to "forge-1.18.2",
                "\$folder" to "forge/forge-1.18.2",
                "\$loader" to "forge",
                "\$fileNameWithoutExtension" to "",
                "\$fileName" to ""
            ), forward = true
        )
        val newLines = helper.extracted(lines, lineCtx,File(""))
        assertEquals(newLines!!.joinToString("\n"), """
// IF <= forge-1.19
        renderBackground(matrixStack);
        drawCenteredString(matrixStack, this.font,
                title, this.width / 2, this.height / 4 - 16, 16764108);
// ELSE
// IF forge-1.20
//        renderBackground(matrixStack);
// ELSE
//         renderBackground(matrixStack, mouseX, mouseY, partialTicks);
// END IF
//        matrixStack.drawCenteredString(this.font,
//                title, this.width / 2, this.height / 4 - 16, 16764108);
// END IF
        """.trimIndent())
    }
}