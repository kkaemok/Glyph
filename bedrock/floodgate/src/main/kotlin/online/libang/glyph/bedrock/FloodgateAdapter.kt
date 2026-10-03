package online.libang.glyph.bedrock

import online.libang.glyph.api.bukkit.bedrock.BedrockAdapter
import org.geysermc.floodgate.api.FloodgateApi
import java.util.*

class FloodgateAdapter : BedrockAdapter {
    override fun isBedrockPlayer(uuid: UUID): Boolean {
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid)
    }
}