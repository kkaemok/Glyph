package online.libang.glyph.transport.paper

import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import net.minecraft.network.protocol.game.ClientboundBossEventPacket

/** Consumed packet writes complete their promise; no packet encoding or ByteBuf copies. */
internal class BossBarInterceptor(private val transport: BossBarHudTransport) : ChannelDuplexHandler() {
    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        if (msg is ClientboundBossEventPacket && transport.intercept(msg)) promise.trySuccess()
        else super.write(ctx, msg, promise)
    }
}
