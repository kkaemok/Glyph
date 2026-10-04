package online.libang.glyph.manager

import kr.toxicity.hud.api.component.WidthComponent
import kr.toxicity.hud.api.plugin.ReloadInfo
import online.libang.glyph.element.ImageElement
import online.libang.glyph.image.enums.ImageType
import online.libang.glyph.layout.HudLayout
import online.libang.glyph.pack.PackGenerator
import online.libang.glyph.resource.GlobalResource
import online.libang.glyph.util.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object ImageManager : GlyphManager {

    override val managerName: String = "Image"
    override val supportExternalPacks: Boolean = true

    private val imageMap = HashMap<String, ImageElement>()
    private val imageNameComponent = ConcurrentHashMap<HudLayout.Identifier, WidthComponent>()

    val allImage get() = imageMap.values

    @Synchronized
    fun getImage(group: HudLayout.Identifier) = imageNameComponent[group]
    @Synchronized
    fun setImage(group: HudLayout.Identifier, component: WidthComponent) {
        imageNameComponent[group] = component
    }

    override fun start() {
    }

    fun getImage(name: String) = synchronized(imageMap) {
        imageMap[name]
    }

    override fun preReload() {
        imageMap.clear()
    }

    override fun reload(workingDirectory: File, info: ReloadInfo, resource: GlobalResource) {
        val assets = workingDirectory.subFolder("assets")
        val map = HashMap<String, ImageElement>()
        workingDirectory.subFolder("images").forEachAllYaml(info.sender) { file, s, yamlObject ->
            runCatching {
                if (yamlObject.getAsString("type", "").equals("nine_slice", true) && yamlObject["sizes"] != null) {
                    val sizes = yamlObject["sizes"]!!.asObject().toList()
                    require(sizes.isNotEmpty() && sizes.size <= 128) { "nine-slice sizes must contain 1..128 variants" }
                    for ((size, definition) in sizes) {
                        val id = "$s/$size"
                        val variant = yamlObject.get().toMutableMap().apply {
                            remove("sizes")
                            putAll(definition.asObject().get())
                        }
                        val image = ImageType.NINE_SLICE.createElement(assets, info.sender, file, id,
                            online.libang.glyph.yaml.YamlObjectImpl("$s.sizes.$size", variant))
                        require(map.putIfAbsent(id, image) == null) { "Duplicate image: $id" }
                    }
                    return@runCatching
                }
                if (yamlObject.getAsString("type", "").equals("directory", true)) {
                    val root = assets.toPath().toAbsolutePath().normalize()
                    val folder = root.resolve(yamlObject["directory"]?.asString().ifNull { "directory value not set" }).normalize()
                    require(folder.startsWith(root) && java.nio.file.Files.isDirectory(folder)) { "Image directory must exist inside assets" }
                    java.nio.file.Files.walk(folder).use { paths ->
                        paths.filter { java.nio.file.Files.isRegularFile(it) && it.toString().endsWith(".png", true) }.sorted().forEach { path ->
                            require(path.toRealPath().startsWith(root.toRealPath())) { "Image directory symlink escapes assets: $path" }
                            val suffix = folder.relativize(path).toString().replace('\\', '/').removeSuffix(".png")
                            val id = "$s/$suffix"
                            require(!map.containsKey(id)) { "Duplicate image: $id" }
                            map[id] = ImageElement(id, listOf(path.toFile().toImage().removeEmptySide()
                                .ifNull { "Empty directory image: $path" }.toNamed("${id.replace('/', '_')}.png")),
                                ImageType.SINGLE, yamlObject["setting"]?.asObject() ?: ImageType.emptySetting)
                        }
                    }
                    return@runCatching
                }
                val image = ImageType.valueOf(
                    yamlObject["type"]?.asString().ifNull { "type value not set." }.uppercase()
                ).createElement(assets, info.sender, file, s, yamlObject)
                map.putSync("image") {
                    image
                }
            }.handleFailure(info) {
                "Unable to load this image: $s in ${file.name}"
            }
        }
        map.values.forEach { value ->
            val list = value.image
            if (list.isNotEmpty()) {
                list.distinctBy {
                    it.name
                }.forEach {
                    PackGenerator.addTask(resource.textures + it.name) {
                        it.image.image.toByteArray()
                    }
                }
            }
        }
        imageMap += map
    }

    override fun postReload() {
        imageNameComponent.clear()
    }

    override fun end() {
    }
}
