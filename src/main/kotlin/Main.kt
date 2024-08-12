package io.github.kituin.modmultiversiontool

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.*

/**
 * 遍历指定文件下的所有文件，并返回一个包含所有文件的序列。
 *
 * @param file 指定的文件或目录
 * @return 一个包含所有文件的序列
 */
private fun listFiles(file: File): Sequence<File> {
    return file.walk().filter { it.isFile }
}

/**
 * 检查指定路径是否为有效的加载器目录。
 *
 * @param path 指定的文件或目录
 * @param loader 加载器的名称
 * @return 如果路径不存在或不是文件夹，则返回true，否则返回false
 */
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

/**
 * 复制加载器目录中的版本文件到目标路径。
 *
 * @param loaderDir 加载器目录
 * @param loader 加载器的名称
 * @param targetFileString 目标文件路径字符串
 * @param sourceFile 源文件
 * @param fileHelper 文件操作辅助类
 * @param global 是否为全局加载器
 */
private fun copyLoaderVersionFile(
    loaderDir: File,
    loader: String,
    targetFileString: String,
    sourceFile: File,
    fileHelper: FileHelper,
    global: Boolean = false,
    modAlias: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
) {
    loaderDir.walk()
        .maxDepth(1)
        .filter { it.isDirectory && it.name != "origin" && it != loaderDir }
        .forEach {
            val targetFilePath = it.toPath() / targetFileString
            fileHelper.copy(sourceFile, targetFilePath, it.name, loader, modAlias)
            println("| Info | " + (if (global) "Global" else loader) + " -> ${it.name} | $targetFileString copied")
        }
}

/**
 * 复制加载器目录中的版本文件。
 *
 * @param loaderPathFile 加载器路径文件
 * @param loader 加载器的名称
 * @param fileHelper 文件操作辅助类
 */
private fun copyLoaderVersion(
    loaderPathFile: File,
    loader: String,
    fileHelper: FileHelper,
    modAlias: MutableMap<String, MutableMap<String, String>>
) {
    val originFile = File(loaderPathFile, "origin")
    listFiles(originFile)
        .forEach { sourceFile ->
            val targetFileString = sourceFile.toRelativeString(originFile)
            copyLoaderVersionFile(loaderPathFile, loader, targetFileString, sourceFile, fileHelper, modAlias = modAlias)
        }
}

/**
 * 复制全局加载器目录中的版本文件到其他加载器目录。
 *
 * @param globalOriginFile 全局加载器目录中的origin文件
 * @param loaders 加载器的名称列表
 * @param fileHelper 文件操作辅助类
 */
fun copyGlobalOriginFile(
    globalOriginFile: File,
    loaders: List<String>,
    fileHelper: FileHelper,
    modAlias: MutableMap<String, MutableMap<String, String>>
) {
    listFiles(globalOriginFile).forEach { sourceFile ->
        val targetFileString = sourceFile.toRelativeString(globalOriginFile)
        for (loader in loaders) {
            val loaderPath = Path(fileHelper.projectPath, loader)
            if ((loaderPath / "origin" / targetFileString).exists()) continue
            copyLoaderVersionFile(loaderPath.toFile(), loader, targetFileString, sourceFile, fileHelper, true, modAlias)
        }
    }
}

/**
 * 解析文件ModMultiLoaders.xml
 *
 * @param file 要解析的XML文件
 * @return 包含所有option元素value属性值的列表
 */
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

/**
 * 解析文件ModAliasState.xml
 *
 * @param file 要解析的XML文件
 */
fun parseXmlToModAliasState(file: File): MutableMap<String, MutableMap<String, String>> {
    val map = mutableMapOf<String, MutableMap<String, String>>()
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document: Document = documentBuilder.parse(file)
    document.documentElement.normalize()

    val aliasList: NodeList = document.getElementsByTagName("entry")
    for (i in 0 until aliasList.length) {
        val mapNode = (aliasList.item(i) as Element)
        val key = mapNode.getAttribute("key")
        if (!mapNode.getAttribute("value").isNullOrEmpty()) continue
        val innerMap = mutableMapOf<String, String>()
        val inners = mapNode.getElementsByTagName("entry")
        for (j in 0 until inners.length) {
            val innerEntryNode = inners.item(j) as Element
            val innerKey = innerEntryNode.getAttribute("key")
            val innerValue = innerEntryNode.getAttribute("value")
            innerMap[innerKey] = innerValue
        }
        map[key] = innerMap
    }
    return map
}


fun main() {
    val path = System.getProperty("user.dir")
    println("| Info | 项目目录: $path")
    val modMultiLoaders = File(path, ".idea/ModMultiLoaders.xml")
    val loaders = if (modMultiLoaders.exists()) parseXmlToList(modMultiLoaders)
    else mutableListOf("fabric", "forge", "neoforge", "quilt")
    println("| Info | 读取多版本加载器文件夹配置: $loaders")
    val modAliasState = File(path, ".idea/ModAliasState.xml")
    val modAlias = if (modAliasState.exists()) parseXmlToModAliasState(modMultiLoaders)
    else mutableMapOf()
    println("| Info | 读取变量替换配置: $modAlias")
    val fileHelper = FileHelper(path)
    println("| Info | 开始进行多版本复制")
    val globalOriginFile = File(path, "origin")
    if (!globalOriginFile.exists()) println("| Warn | 全局文件夹不存在,跳过")
    else copyGlobalOriginFile(globalOriginFile, loaders, fileHelper, modAlias)
    for (loader in loaders) {
        val loaderPath = File(path, loader)
        if (checkLoaderDir(loaderPath, loader)) continue
        copyLoaderVersion(loaderPath, loader, fileHelper, modAlias)
    }
    println("| Info | 多版本复制结束")
}