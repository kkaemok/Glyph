package online.libang.glyph.pack

import online.libang.glyph.api.plugin.ReloadInfo
import online.libang.glyph.manager.ConfigManagerImpl
import online.libang.glyph.util.*
import online.libang.glyph.util.forEach
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object PackGenerator {
    private val tasks = TreeMap<String, MutableList<Pair<String, PackFile>>>()

    fun generate(info: ReloadInfo): Map<String, ByteArray> {
        val sender = info.sender
        val resourcePack = runCatching {
            var meta = PackMeta.default
            ConfigManagerImpl.mergeOtherFolders.forEach {
                val mergeTarget = DATA_FOLDER.parentFile.subFolder(it)
                when {
                    mergeTarget.isDirectory -> mergeFolder(mergeTarget) { subMeta ->
                        meta += subMeta
                    }
                    mergeTarget.extension == "zip" -> mergeZip(mergeTarget) { subMeta ->
                        meta += subMeta
                    }
                }
            }
            PackOverlay.entries.forEach {
                it.loadAssets()
            }
            addTask(listOf("pack.mcmeta")) {
                meta.toByteArray()
            }
            BOOTSTRAP.resource("icon.png")?.buffered()?.use {
                val read = it.readAllBytes()
                addTask(listOf("pack.png")) {
                    read
                }
            }
            val compiler = CompiledPack.Builder()
            val snapshot = synchronized(tasks) { tasks.values.flatMap { it.toList() } }
            for ((origin, task) in snapshot) compiler.add(task.path, task(), origin)
            compiler.build().files

        }.getOrElse {
            it.handle(sender, "Unable to make a resource pack.")
            synchronized(tasks) { tasks.clear() }
            throw it
        }
        synchronized(tasks) { tasks.clear() }
        return resourcePack
    }

    private fun mergeFolder(mergeTarget: File, metaBlock: (PackMeta) -> Unit) {
        val mergeLength = mergeTarget.path.length + 1
        fun addFile(target: File) {
            if (target.isDirectory) target.forEach { t ->
                addFile(t)
            } else {
                addTask(target.path.substring(mergeLength).split(File.separatorChar), mergeTarget.path) {
                    target.inputStream().buffered().use { stream ->
                        stream.readAllBytes()
                    }
                }
            }
        }
        mergeTarget.forEach { target ->
            if (target.name == "pack.mcmeta") {
                runCatching {
                    metaBlock(target.toMcmeta())
                }.getOrElse { e ->
                    e.handle("Invalid pack.mcmeta: ${target.path}")
                }
            } else addFile(target)
        }
    }

    private fun mergeZip(mergeTarget: File, metaBlock: (PackMeta) -> Unit) {
        ZipInputStream(mergeTarget.inputStream().buffered()).use {
            var entry: ZipEntry?
            do {
                entry = it.nextEntry
                entry?.let { e ->
                    if (e.isDirectory) { it.closeEntry(); return@let }
                    val read = it.readAllBytes()
                    if (e.name == "pack.mcmeta") {
                        runCatching {
                            metaBlock(read.toMcmeta())
                        }.getOrElse { e ->
                            e.handle("Invalid pack.mcmeta: ${mergeTarget.path}")
                        }
                    } else {
                        addTask(e.name.split('/'), mergeTarget.path) {
                            read
                        }
                    }
                    it.closeEntry()
                }
            } while (entry != null)
        }
    }

    fun addTask(dir: Iterable<String>, origin: String = "Glyph", byteArray: () -> ByteArray) {
        val path = dir.joinToString("/")
        CompiledPack.validatePath(path)
        synchronized(tasks) {
            tasks.getOrPut(path) { ArrayList() }.add(origin to PackFile(path, byteArray))
        }
    }
}
