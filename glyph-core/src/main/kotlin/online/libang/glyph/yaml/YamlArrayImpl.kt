package online.libang.glyph.yaml

import online.libang.glyph.api.yaml.YamlArray
import online.libang.glyph.api.yaml.YamlElement
import online.libang.glyph.util.toYaml
import java.util.*

class YamlArrayImpl(
    path: String,
    private val list: List<*>
) : YamlConfigurationImpl(path), YamlArray {

    @Suppress("UNCHECKED_CAST")
    override fun get(): MutableList<Any> = Collections.unmodifiableList(list as MutableList<Any>)


    override fun iterator(): MutableIterator<YamlElement> {
        val returnList = ArrayList<YamlElement>()
        list.forEach {
            if (it != null) returnList += it.toYaml(path())
        }
        return returnList.iterator()
    }
}