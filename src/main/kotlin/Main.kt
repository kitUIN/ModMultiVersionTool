package io.github.kituin.modmultiversiontool

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.*

private fun listFiles(file: File): Sequence<File> {
    return file.walk().filter { it.isFile }
}

private fun checkLoaderDir(path: File, loader: String): Boolean {
    if (!path.exists()) {
        println("| Warn | ${loader}为空,跳过")
        return true
    }
    if (path.isFile) {
        println("| Warn | ${loader}不是文件夹,跳过")
        return true
    }
    return false
}


private fun copyLoaderVersionFile(
    loaderDir: File, loader: String, targetFileString: String, sourceFile: File,
    fileHelper: FileHelper, global: Boolean = false
) {
    loaderDir.walk()
        .maxDepth(1)
        .filter { it.isDirectory && it.name != "origin" && it != loaderDir }
        .forEach {
            val targetFilePath = it.toPath() / targetFileString
            fileHelper.copy(sourceFile, targetFilePath, it.name, loader)
            println("| Info | " + (if (global) "Global" else loader) + " -> ${it.name} | $targetFileString copied")
        }
}
private fun copyLoaderVersion(loaderPathFile: File, loader: String, fileHelper: FileHelper) {
    val originFile = File(loaderPathFile, "origin")
    listFiles(originFile)
        .forEach { sourceFile ->
            val targetFileString = sourceFile.toRelativeString(originFile)
            copyLoaderVersionFile(loaderPathFile, loader, targetFileString, sourceFile, fileHelper)
        }
}
fun copyGlobalOriginFile(globalOriginFile: File, loaders: List<String>, fileHelper: FileHelper) {
    listFiles(globalOriginFile).forEach { sourceFile ->
        val targetFileString = sourceFile.toRelativeString(globalOriginFile)
        for (loader in loaders) {
            val loaderPath = Path(fileHelper.projectPath, loader)
            if ((loaderPath / "origin" / targetFileString).exists()) continue
            copyLoaderVersionFile(loaderPath.toFile(), loader, targetFileString, sourceFile, fileHelper, true)
        }
    }
}
fun parseXmlToList(file: File): List<String> {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val document = builder.parse(file)
    val optionElements = document.getElementsByTagName("option")
    val list = mutableListOf<String>()

    for (i in 0 until optionElements.length) {
        val element = optionElements.item(i) as Element
        val value = element.getAttribute("value")
        list.add(value)
    }

    return list
}
fun main() {
    val path = System.getProperty("user.dir")
    println("| Info | 项目目录: $path")
    val modMultiLoaders = File(path,".idea/ModMultiLoaders.xml")
    val loaders= if(modMultiLoaders.exists()) parseXmlToList(modMultiLoaders)
    else mutableListOf("fabric", "forge", "neoforge", "quilt")
    println("| Info | 读取多版本加载器文件夹配置: $loaders")
    val fileHelper = FileHelper(path)
    println("| Info | 开始进行多版本复制")
    val globalOriginFile = File(path, "origin")
    if (!globalOriginFile.exists()) println("| Warn | 全局文件夹不存在,跳过")
    else copyGlobalOriginFile(globalOriginFile, loaders, fileHelper)
    for (loader in loaders) {
        val loaderPath = File(path, loader)
        if (checkLoaderDir(loaderPath, loader)) continue
        copyLoaderVersion(loaderPath, loader, fileHelper)
    }
    println("| Info | 多版本复制结束")
}