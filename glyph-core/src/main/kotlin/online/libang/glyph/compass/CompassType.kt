package online.libang.glyph.compass

import online.libang.glyph.api.yaml.YamlObject
import online.libang.glyph.compass.type.CircleCompass
import online.libang.glyph.resource.GlobalResource
import java.io.File

enum class CompassType {
    CIRCLE {
        override fun build(
            resource: GlobalResource,
            assets: File,
            name: String,
            section: YamlObject
        ): CompassImpl {
            return CircleCompass(resource, assets, name, section)
        }
    }
    ;

    abstract fun build(resource: GlobalResource, assets: File, name: String, section: YamlObject): CompassImpl
}