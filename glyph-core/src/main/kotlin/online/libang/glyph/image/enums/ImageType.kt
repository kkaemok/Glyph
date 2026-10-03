package online.libang.glyph.image.enums

import kr.toxicity.command.BetterCommandSource
import online.libang.glyph.api.component.PixelComponent
import online.libang.glyph.api.listener.HudListener
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.element.ImageElement
import online.libang.glyph.image.ImageComponent
import online.libang.glyph.util.*
import online.libang.glyph.yaml.YamlObjectImpl
import java.io.File
import java.util.regex.Pattern
import kotlin.math.roundToInt
import kotlin.text.replace

enum class ImageType {
    SINGLE {
        override fun getComponent(listener: HudListener, frame: Long, component: ImageComponent, player: HudPlayer): PixelComponent {
            val get = listener.getValue(player).run {
                if (isNaN()) 0 else (this * component.images.lastIndex).roundToInt()
            }
            return if (component.images.isNotEmpty()) {
                if (get >= 0) component.images[get
                    .coerceAtLeast(0)
                    .coerceAtMost(component.images.lastIndex)] else component.images[0]
            } else EMPTY_PIXEL_COMPONENT
        }

        override fun createElement(
            assets: File,
            sender: BetterCommandSource,
            file: File,
            s: String,
            yamlObject: YamlObject
        ): ImageElement {
            val fileName = yamlObject["file"]?.asString().ifNull { "file value not set." }
                .replace('/', File.separatorChar)
            val targetFile = File(
                assets,
                fileName
            )
            return ImageElement(
                s,
                listOf(
                    targetFile
                        .toImage()
                        .flip(yamlObject.toFlip())
                        .removeEmptySide()
                        .ifNull { "Invalid image." }
                        .toNamed(fileName.replace(File.separatorChar, '_')),
                ),
                this,
                yamlObject["setting"]?.asObject() ?: emptySetting
            )
        }

    },
    NINE_SLICE {
        override fun getComponent(listener: HudListener, frame: Long, component: ImageComponent, player: HudPlayer) =
            SINGLE.getComponent(listener, frame, component, player)

        override fun createElement(assets: File, sender: BetterCommandSource, file: File, s: String, yamlObject: YamlObject): ImageElement {
            val name = yamlObject["file"]?.asString().ifNull { "nine-slice file value not set" }
            val source = File(assets, name).toImage()
            val border = yamlObject["border"]?.asObject().ifNull { "nine-slice border value not set" }
            val insets = online.libang.glyph.image.NineSlice.Insets(
                border.getAsInt("left", 0), border.getAsInt("top", 0), border.getAsInt("right", 0), border.getAsInt("bottom", 0))
            val width = yamlObject.getAsInt("width", source.width)
            val height = yamlObject.getAsInt("height", source.height)
            val mode = online.libang.glyph.image.NineSlice.Mode.valueOf(yamlObject.getAsString("mode", "stretch").uppercase())
            val image = online.libang.glyph.image.NineSlice.resize(source, width, height, insets, mode)
            return ImageElement(s, listOf(online.libang.glyph.image.LoadedImage(image, 0, 0)
                .toNamed("${s.replace('/', '_')}_${width}x${height}_${mode.name.lowercase()}.png")), this,
                yamlObject["setting"]?.asObject() ?: emptySetting)
        }
    },
    LISTENER {
        override fun getComponent(listener: HudListener, frame: Long, component: ImageComponent, player: HudPlayer): PixelComponent {
            val get = listener.getValue(player).run {
                if (isNaN()) 0 else (this * component.images.lastIndex).roundToInt()
            }
            return if (get >= 0) component.images[get
                .coerceAtLeast(0)
                .coerceAtMost(component.images.lastIndex)] else component choose frame
        }

        override fun createElement(
            assets: File,
            sender: BetterCommandSource,
            file: File,
            s: String,
            yamlObject: YamlObject
        ): ImageElement {
            val splitType = yamlObject["split-type"]?.asString()?.let { splitType ->
                runCatching {
                    SplitType.valueOf(splitType.uppercase())
                }.onFailure {
                    it.handle("Unable to find that split-type: $splitType")
                }.getOrNull()
            } ?: SplitType.LEFT
            val split = yamlObject.getAsInt("split", 25).coerceAtLeast(1)
            val fileName = yamlObject["file"]?.asString().ifNull { "file value not set." }
                .replace('/', File.separatorChar)
            val getFile = File(
                assets,
                fileName
            )
            return ImageElement(
                s,
                splitType.split(
                    getFile
                        .toImage()
                        .flip(yamlObject.toFlip())
                        .removeEmptySide()
                        .ifNull { "Invalid image." }
                        .toNamed("${fileName.replace(File.separatorChar, '_').substringBeforeLast('.')}_${splitType.name.lowercase()}_$split.png"), split
                ),
                this,
                yamlObject["setting"]?.asObject()
                    .ifNull { "setting configuration not found." }
            )
        }
    },
    SEQUENCE {
        override fun getComponent(listener: HudListener, frame: Long, component: ImageComponent, player: HudPlayer): PixelComponent {
            val get = listener.getValue(player).run {
                if (isNaN()) 0 else (this * component.images.lastIndex).roundToInt()
            }
            return if (get >= 0) component.images[get
                .coerceAtLeast(0)
                .coerceAtMost(component.images.lastIndex)] else component choose frame
        }

        override fun createElement(
            assets: File,
            sender: BetterCommandSource,
            file: File,
            s: String,
            yamlObject: YamlObject
        ): ImageElement {
            val globalFrame = yamlObject.getAsInt("frame", 1).coerceAtLeast(1)
            return ImageElement(
                s,
                (yamlObject["files"]?.asArray()?.map {
                    it.asString()
                } ?: emptyList()).ifEmpty {
                    throw RuntimeException("files are empty.")
                }.flatMap { string ->
                    val matcher = multiFrameRegex.matcher(string)
                    var fileName = string
                    var frame = 1
                    if (matcher.find()) {
                        fileName = matcher.group("name")
                        frame = matcher.group("frame").toInt()
                    }
                    fileName = fileName.replace('/', File.separatorChar)
                    val targetFile = File(assets, fileName)
                    val targetImage = targetFile
                        .toImage()
                        .flip(yamlObject.toFlip())
                        .removeEmptyWidth()
                        .ifNull { "Invalid image: $string" }
                        .toNamed(fileName.replace(File.separatorChar, '_'))
                    (0..<(frame * globalFrame).coerceAtLeast(1)).map {
                        targetImage
                    }
                },
                this,
                yamlObject["setting"]?.asObject() ?: emptySetting
            )
        }
    }
    ;
    companion object {
        val emptySetting = YamlObjectImpl("", mutableMapOf<String, Any>())
        private val multiFrameRegex = Pattern.compile("(?<name>(([a-zA-Z]|/|.|(_))+)):(?<frame>([0-9]+))")

        private fun YamlObject.toFlip() = get("flip")?.asArray()?.map {
            FlipType.valueOf(it.asString().uppercase())
        }?.toSet() ?: emptySet()
    }

    abstract fun getComponent(listener: HudListener, frame: Long, component: ImageComponent, player: HudPlayer): PixelComponent
    abstract fun createElement(assets: File, sender: BetterCommandSource, file: File, s: String, yamlObject: YamlObject): ImageElement
}
