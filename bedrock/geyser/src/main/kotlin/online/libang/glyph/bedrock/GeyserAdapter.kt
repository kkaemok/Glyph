package online.libang.glyph.bedrock

import online.libang.glyph.api.bukkit.bedrock.BedrockAdapter
import org.geysermc.api.Geyser
import java.util.*

class GeyserAdapter : BedrockAdapter {
    override fun isBedrockPlayer(uuid: UUID): Boolean {
        return Geyser.api().isBedrockPlayer(uuid)
    }
}