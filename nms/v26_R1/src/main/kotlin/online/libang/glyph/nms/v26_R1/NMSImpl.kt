package online.libang.glyph.nms.v26_R1

import com.mojang.authlib.GameProfile
import io.papermc.paper.datacomponent.DataComponentType
import io.papermc.paper.event.server.ServerResourcesReloadedEvent
import kr.toxicity.command.BetterCommandSource
import kr.toxicity.command.impl.CommandModule
import online.libang.glyph.api.Glyph
import online.libang.glyph.api.GlyphAPI
import online.libang.glyph.api.bukkit.nms.NMS
import online.libang.glyph.api.bukkit.nms.NMSVersion
import online.libang.glyph.api.component.WidthComponent
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.nms.v26_R1.entity.CraftEntityView
import online.libang.glyph.nms.v26_R1.entity.CraftLivingEntityView
import online.libang.glyph.nms.v26_R1.entity.createAdaptedFieldGetter
import online.libang.glyph.nms.v26_R1.entity.unsafeHandle
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key
import net.kyori.adventure.pointer.Pointers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.format.TextDecoration
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.Connection
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.server.network.ServerGamePacketListenerImpl
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.WorldBorder
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.*
import org.bukkit.permissions.Permission
import org.bukkit.plugin.Plugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

class NMSImpl : NMS {
    companion object {
        private const val INJECT_NAME = Glyph.DEFAULT_NAMESPACE
        private val bossBarMap = ConcurrentHashMap<UUID, online.libang.glyph.transport.paper.BossBarHudTransport>()

        private val getGameProfile: (net.minecraft.world.entity.player.Player) -> GameProfile = createAdaptedFieldGetter { it.gameProfile }
        private val getConnection: (ServerCommonPacketListenerImpl) -> Connection = createAdaptedFieldGetter { it.connection }


    }

    override fun inject(player: HudPlayer, color: BossBar.Color) {
        val h = player.handle() as CraftPlayer
        bossBarMap.computeIfAbsent(h.uniqueId) {
            online.libang.glyph.transport.paper.BossBarHudTransport(h, h.handle.connection, getConnection(h.handle.connection).channel, color)
        }
    }
    override fun showBossBar(player: HudPlayer, color: BossBar.Color, component: Component) {
        bossBarMap[player.uuid()]?.update(color, component)
    }

    override fun removeBossBar(player: HudPlayer) {
        bossBarMap.remove(player.uuid())?.close()
    }

    override fun getVersion(): NMSVersion {
        return NMSVersion.V26_R1
    }

    override fun getTextureValue(player: HudPlayer): String {
        val value = getGameProfile((player.handle() as CraftPlayer).handle)
            .properties["textures"]
        return if (value.isNotEmpty()) value.first().value else ""
    }

    override fun registerCommand(module: CommandModule<BetterCommandSource>) {
        val dispatcher = (Bukkit.getServer() as CraftServer).server.commands.dispatcher
        val bootstrap = GlyphAPI.inst().bootstrap()
        module.build { s: CommandSourceStack ->
            when (val sender = s.bukkitSender) {
                is ConsoleCommandSender -> bootstrap.consoleSource()
                is Player -> GlyphAPI.inst().playerManager.getHudPlayer(sender.uniqueId)
                else -> null
            }
        }.forEach {
            dispatcher.register(it)
        }
    }

    override fun handleReloadCommand(module: CommandModule<BetterCommandSource>) {
        val bootstrap = GlyphAPI.inst().bootstrap()
        Bukkit.getPluginManager().registerEvents(object : Listener {
            @EventHandler
            fun ServerResourcesReloadedEvent.reload() {
                registerCommand(module)
            }
        }, bootstrap as Plugin)
    }

    override fun getFoliaAdaptedEntity(entity: Entity): Entity {
        return when (entity) {
            is Player -> getFoliaAdaptedPlayer(entity)
            is CraftLivingEntity -> CraftLivingEntityView(entity)
            else -> CraftEntityView(entity as CraftEntity)
        }
    }

    @Suppress("UnstableApiUsage")
    override fun getFoliaAdaptedPlayer(player: Player): Player {
        val craftPlayer = player as CraftPlayer
        return object : CraftPlayer(Bukkit.getServer() as CraftServer, craftPlayer.unsafeHandle as ServerPlayer) {
            override fun getPersistentDataContainer(): CraftPersistentDataContainer {
                return player.persistentDataContainer
            }
            override fun getHandle(): ServerPlayer {
                return craftPlayer.unsafeHandle as ServerPlayer
            }
            override fun dropItem(dropAll: Boolean): Boolean {
                return player.dropItem(dropAll)
            }
            override fun dropItem(p0: Int, p1: Int, p2: Boolean, p3: Consumer<Item>?): Item? {
                return player.dropItem(p0, p1, p2, p3)
            }
            override fun dropItem(p0: EquipmentSlot, p1: Int, p2: Boolean, p3: Consumer<Item>?): Item? {
                return player.dropItem(p0, p1, p2, p3)
            }
            override fun dropItem(p0: ItemStack, p1: Boolean, p2: Consumer<Item>?): Item? {
                return player.dropItem(p0, p1, p2)
            }
            override fun getHealth(): Double {
                return player.health
            }
            override fun getScaledHealth(): Float {
                return player.scaledHealth
            }
            override fun getHealthScale(): Double {
                return player.healthScale
            }
            override fun getUniqueId(): UUID = player.uniqueId
            override fun getFirstPlayed(): Long {
                return player.firstPlayed
            }
            override fun getInventory(): PlayerInventory {
                return player.inventory
            }
            override fun getEnderChest(): Inventory {
                return player.enderChest
            }
            override fun getCooldown(key: Key): Int {
                return player.getCooldown(key)
            }
            override fun setCooldown(key: Key, i: Int) {
                player.setCooldown(key, i)
            }
            override fun isOp(): Boolean {
                return player.isOp
            }
            override fun getGameMode(): GameMode {
                return player.gameMode
            }
            override fun getEquipment(): EntityEquipment {
                return player.equipment
            }
            override fun hasPermission(name: String): Boolean {
                return player.hasPermission(name)
            }
            override fun hasPermission(perm: Permission): Boolean {
                return player.hasPermission(perm)
            }
            override fun isPermissionSet(name: String): Boolean {
                return player.isPermissionSet(name)
            }
            override fun isPermissionSet(perm: Permission): Boolean = player.isPermissionSet(perm)
            override fun hasPlayedBefore(): Boolean = player.hasPlayedBefore()
            override fun getLastDeathLocation(): Location? = player.lastDeathLocation
            override fun getLocation(): Location = player.location
            override fun getName(): String = player.name
            override fun isOnGround(): Boolean = player.isOnGround
            override fun getLastDamageCause(): EntityDamageEvent? = player.lastDamageCause
            override fun getWorldBorder(): WorldBorder? = player.worldBorder
            override fun pointers(): Pointers = player.pointers()
            override fun spigot(): Player.Spigot = player.spigot()

            override fun showBossBar(bar: BossBar) {
                player.showBossBar(bar)
            }
            override fun hideBossBar(bar: BossBar) {
                player.hideBossBar(bar)
            }
            override fun sendMessage(message: String) {
                player.sendMessage(message)
            }

            override fun <T : Any> getData(p0: DataComponentType.Valued<T>): T? = player.getData(p0)
            override fun <T : Any> getDataOrDefault(
                p0: DataComponentType.Valued<out T>,
                p1: T?
            ): T? = player.getDataOrDefault(p0, p1)
            override fun hasData(p0: DataComponentType): Boolean = player.hasData(p0)
        }
    }


}
