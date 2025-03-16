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
    @Test
    fun testSaBug() {
        val lines = """
package io.github.kituin.chatimage.mixin;

import com.google.common.collect.Lists;
import io.github.kituin.chatimage.tool.ChatImageStyle;
import io.github.kituin.ChatImageCode.ChatImageBoolean;
import io.github.kituin.ChatImageCode.ChatImageCode;
import io.github.kituin.ChatImageCode.ChatImageCodeTool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.Objects;

import static io.github.kituin.ChatImageCode.ChatImageCodeInstance.LOGGER;
import static io.github.kituin.ChatImageCode.ChatImageCodeInstance.createBuilder;
import static io.github.kituin.chatimage.tool.ChatImageStyle.SHOW_IMAGE;
import static io.github.kituin.chatimage.tool.SimpleUtil.*;


/**
 * 注入修改文本显示,自动将CICode转换为可鼠标悬浮格式文字
 *
 * @author kitUIN
 */
@Mixin(#ChatComponent#.class)
public class #kituinChatComponentMixinClass# {
    @Shadow
    @Final
    private #MinecraftClient#;

    @ModifyVariable(at = @At("HEAD"),
            method = "#kituinaddMessageMixin#",
            argsOnly = true)
    public #Component# addMessage(#Component# message) {
        if (#kituinChatImageConfig#.experimentalTextComponentCompatibility) {
            StringBuilder sb = new StringBuilder();
            #Component# temp = chatImageflattenTree(message, sb, false);
            ChatImageBoolean allString = new ChatImageBoolean(true);
            ChatImageCodeTool.sliceMsg(sb.toString(), true, allString, (e) -> LOGGER.error(e.getMessage()));
            if (!allString.isValue()) message = temp;
        }
        return chatimagereplaceMessage(message);
    }

// IF >= fabric-1.19
//    @Unique
//    private #Component#Content chatImagegetContents(#Component# text){
//        return text.getContent();
//    }
// ELSE IF >= forge-1.19 || > neoforge-1.20.1
//    @Unique
//    private #Component#Contents chatImagegetContents(#Component# text){
//        return text.getContents();
//    }
// ELSE
//    @Unique
//    private #Component# chatImagegetContents(#Component# text) {
//        return text;
//    }
// END IF
        """.trimIndent().split("\n")
        val lineCtx = LineCtx(
            Path("").toFile(), mutableMapOf(
                "$$" to "forge-1.19.2",
                "\$folder" to "forge/forge-1.19.2",
                "\$loader" to "forge",
                "\$fileNameWithoutExtension" to "",
                "\$fileName" to ""
            ), forward = true
        )
        val newLines = helper.extracted(lines, lineCtx,File(".java"))
        assertEquals(newLines!!.joinToString("\n"), """
package io.github.kituin.chatimage.mixin;

import com.google.common.collect.Lists;
import io.github.kituin.chatimage.tool.ChatImageStyle;
import io.github.kituin.ChatImageCode.ChatImageBoolean;
import io.github.kituin.ChatImageCode.ChatImageCode;
import io.github.kituin.ChatImageCode.ChatImageCodeTool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.Objects;

import static io.github.kituin.ChatImageCode.ChatImageCodeInstance.LOGGER;
import static io.github.kituin.ChatImageCode.ChatImageCodeInstance.createBuilder;
import static io.github.kituin.chatimage.tool.ChatImageStyle.SHOW_IMAGE;
import static io.github.kituin.chatimage.tool.SimpleUtil.*;


/**
 * 注入修改文本显示,自动将CICode转换为可鼠标悬浮格式文字
 *
 * @author kitUIN
 */
@Mixin(#ChatComponent#.class)
public class #kituinChatComponentMixinClass# {
    @Shadow
    @Final
    private #MinecraftClient#;

    @ModifyVariable(at = @At("HEAD"),
            method = "#kituinaddMessageMixin#",
            argsOnly = true)
    public #Component# addMessage(#Component# message) {
        if (#kituinChatImageConfig#.experimentalTextComponentCompatibility) {
            StringBuilder sb = new StringBuilder();
            #Component# temp = chatImageflattenTree(message, sb, false);
            ChatImageBoolean allString = new ChatImageBoolean(true);
            ChatImageCodeTool.sliceMsg(sb.toString(), true, allString, (e) -> LOGGER.error(e.getMessage()));
            if (!allString.isValue()) message = temp;
        }
        return chatimagereplaceMessage(message);
    }

// IF >= fabric-1.19
//    @Unique
//    private #Component#Content chatImagegetContents(#Component# text){
//        return text.getContent();
//    }
// ELSE IF >= forge-1.19 || > neoforge-1.20.1
    @Unique
    private #Component#Contents chatImagegetContents(#Component# text){
        return text.getContents();
    }
// ELSE
//    @Unique
//    private #Component# chatImagegetContents(#Component# text) {
//        return text;
//    }
// END IF
        """.trimIndent())
    }
}