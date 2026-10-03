package online.libang.glyph.manager

import kr.toxicity.command.BetterCommandSource
import kr.toxicity.command.CommandListener
import kr.toxicity.command.SenderType
import kr.toxicity.command.impl.BetterCommand
import kr.toxicity.command.impl.ClassSerializer
import kr.toxicity.command.impl.CommandMessage
import kr.toxicity.command.impl.annotation.*
import online.libang.glyph.api.adapter.LocationWrapper
import online.libang.glyph.api.adapter.WorldWrapper
import online.libang.glyph.api.configuration.HudObjectType
import online.libang.glyph.api.player.HudPlayer
import online.libang.glyph.api.player.PointedLocation
import online.libang.glyph.api.player.PointedLocationSource
import online.libang.glyph.api.plugin.ReloadFlagType
import online.libang.glyph.api.plugin.ReloadInfo
import online.libang.glyph.api.plugin.ReloadState.*
import online.libang.glyph.api.update.UpdateEvent
import online.libang.glyph.command.*
import online.libang.glyph.placeholder.PlaceholderSource
import online.libang.glyph.resource.GlobalResource
import online.libang.glyph.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import java.io.File

object CommandManager : GlyphManager {

    override val managerName: String = "Command"
    override val supportExternalPacks: Boolean = false

    val library = BetterCommand(DATA_FOLDER.subFolder("lang").apply {
        PLUGIN.loadAssets("lang") { name, stream ->
            val file = File(this, name)
            if (!file.exists()) file.outputStream().buffered().use {
                stream.copyTo(it)
            }
        }
    }, MiniMessage.miniMessage(), BOOTSTRAP.logger())
        .exceptionHandler {
            if (ConfigManagerImpl.debug()) {
                warn(
                    "Stack trace:",
                    it.stackTraceToString()
                )
            }
        }
        .addSerializer(HudPlayerStack::class.java, ClassSerializer.builder { _, s ->
            if (s == "all") HudPlayerStack(PlayerManagerImpl.allHudPlayer)
            else PlayerManagerImpl.getHudPlayer(s)?.let {
                HudPlayerStack(listOf(it))
            }
        }.name("player")
            .suggests { PlayerManagerImpl.allHudPlayer.map { it.name() } + "all" }
            .nullMessage(CommandMessage("glyph.null.player", Component.text("Unable to find this player: [value]")))
            .build()
        )
        .addSerializer(HudStack::class.java, ClassSerializer.builder { _, s ->
            when (s) {
                "all" -> HudStack(HudObjectType.HUD.all())
                "default" -> HudStack(HudObjectType.HUD.defaultObjects().toList())
                else -> HudManagerImpl.getHud(s)?.let {
                    HudStack(listOf(it))
                }
            }
        }.name("hud")
            .suggests { HudManagerImpl.allNames.toList() + "all" }
            .nullMessage(CommandMessage("glyph.null.hud", Component.text("Unable to find this hud: [value]")))
            .build()
        )
        .addSerializer(CompassStack::class.java, ClassSerializer.builder { _, s ->
            when (s) {
                "all" -> CompassStack(HudObjectType.COMPASS.all())
                "default" -> CompassStack(HudObjectType.COMPASS.defaultObjects().toList())
                else -> CompassManagerImpl.getCompass(s)?.let {
                    CompassStack(listOf(it))
                }
            }
        }.name("compass")
            .suggests { CompassManagerImpl.allNames.toList() + "all" }
            .nullMessage(CommandMessage("glyph.null.compass", Component.text("Unable to find this compass: [value]")))
            .build()
        )
        .addSerializer(PopupStack::class.java, ClassSerializer.builder { _, s ->
            when (s) {
                "all" -> PopupStack(HudObjectType.POPUP.all())
                "default" -> PopupStack(HudObjectType.POPUP.defaultObjects().toList())
                else -> PopupManagerImpl.getPopup(s)?.let {
                    PopupStack(listOf(it))
                }
            }
        }.name("popup")
            .suggests { PopupManagerImpl.allNames.toList() + "all" }
            .nullMessage(CommandMessage("glyph.null.popup", Component.text("Unable to find this popup: [value]")))
            .build()
        )
        .addSerializer(WorldWrapper::class.java, ClassSerializer.builder { _, s ->
            BOOTSTRAP.world(s)
        }.name("world")
            .suggests { BOOTSTRAP.worlds().map { it.name } }
            .nullMessage(CommandMessage("glyph.null.world", Component.text("Unable to find this world: [value]")))
            .build()
        )
        .addSerializer(Vec3::class.java, ClassSerializer.builder { _, s ->
            val split = s.split('_')
            if (split.size == 3) runCatching { Vec3(split[0].toDouble(), split[1].toDouble(), split[2].toDouble()) }.getOrNull() else null
        }.name("vector")
            .suggests { listOf("0_0_0") }
            .nullMessage(CommandMessage("glyph.null.vector", Component.text("Invalid vector: [value]")))
            .build()
        )
        .addSerializer(CompassIcon::class.java, ClassSerializer.builder { _, s -> CompassIcon(s) }
            .name("icon")
            .suggests { if (it is HudPlayer) it.pointers().map { s -> s.name } else emptyList() }
            .nullMessage(CommandMessage("glyph.null.icon", Component.text("Unable to find this icon: [icon]")))
            .build()
        )
        .silentLog {
            !ConfigManagerImpl.debug()
        }

    @Suppress("UNUSED")
    val module = library.module<BetterCommandSource>("hud")
        .aliases(arrayOf("betterhud", "bh"))
        .permission("hud")
        .executes(object : CommandListener {

            //Reload
            private val reload_tryReload = library.registerKey(CommandMessage("glyph.reload.message.try_reload", Component.text("Trying to reload. please wait...")))
            private val reload_onReload = library.registerKey(CommandMessage("glyph.reload.message.on_reload", Component.text("The plugin is still reloading!")))
            private val reload_success = library.registerKey(CommandMessage("glyph.reload.message.success", Component.text("Reload successful! ([time] ms)")))
            private val reload_failure1 = library.registerKey(CommandMessage("glyph.reload.message.failure.1", Component.text("Reload failed.")))
            private val reload_failure2 = library.registerKey(CommandMessage("glyph.reload.message.failure.2", Component.text("Cause: [cause]")))
            @Command
            @Description(key = "glyph.reload.description", defaultValue = "Reload Glyph.")
            @Aliases(aliases = ["re", "rl"])
            @Permission("glyph.reload")
            fun reload(@Source me: BetterCommandSource, @Option @CanBeNull args: String?) {
                reload_tryReload.send(me)
                asyncTask {
                    when (val reload = PLUGIN.reload(me, *args?.split(' ')?.let { ReloadFlagType.from(it).toTypedArray() } ?: emptyArray())) {
                        is OnReload -> reload_onReload.send(me)
                        is Success -> reload_success.send(me, mapOf("time" to reload.time.withDecimal()))
                        is Failure -> {
                            reload_failure1.send(me)
                            reload_failure2.send(me, mapOf("cause" to Component.text("${reload.throwable.javaClass.simpleName}: ${reload.throwable.message}")))
                        }
                    }
                }
            }
            //Reload

            //Parse
            private val parse_failure1 = library.registerKey(CommandMessage("glyph.parse.message.failure.1", Component.text("Parse failed.")))
            private val parse_failure2 = library.registerKey(CommandMessage("glyph.parse.message.failure.2", Component.text("Cause: [cause]")))
            @Command
            @Description(key = "glyph.reload.parse", defaultValue = "Tests Glyph's text placeholder.")
            @Aliases(aliases = ["p"])
            @Permission("glyph.parse")
            @Sender(type = [SenderType.PLAYER])
            fun parse(@Source me: HudPlayer, @Vararg argument: String) {
                runCatching {
                    me.audience().sendMessage(
                        MiniMessage
                            .miniMessage()
                            .deserialize(PlaceholderManagerImpl.parse(argument, PlaceholderSource.empty)(UpdateEvent.EMPTY)(me))
                    )
                }.onFailure { e ->
                    parse_failure1.send(me)
                    parse_failure2.send(me, mapOf("cause" to Component.text("${e.javaClass.simpleName}: ${e.message}")))
                }
            }
            //Parse

            //Generate
            private val generate_tryGenerate = library.registerKey(CommandMessage("glyph.generate.message.try_generate", Component.text("Trying to generate. please wait...")))
            private val generate_success = library.registerKey(CommandMessage("glyph.generate.message.success", Component.text("The lang file '[name]' is successfully generated.")))
            private val generate_failure = library.registerKey(CommandMessage("glyph.generate.message.failure", Component.text("Generate failure. Maybe [name] already exists.")))
            @Command
            @Description(key = "glyph.generate.description", defaultValue = "Generates default language file.")
            @Aliases(aliases = ["gen"])
            @Permission("glyph.generate")
            fun generate(@Source me: BetterCommandSource) {
                generate_tryGenerate.send(me)
                asyncTask {
                    val locale = me.locale()
                    val localeString = mapOf("name" to Component.text(locale.toString()))
                    if (library.generateDefaultLang(locale)) generate_success.send(me, localeString)
                    else generate_failure.send(me, localeString)
                }
            }
            //Generate
        }).children("hud") {
            it.permission("glyph.hud")
                .description(CommandMessage("glyph.hud.description", Component.text("Manages player's hud.")))
                .executes(object : CommandListener {
                    //Hud add
                    private val add_success = library.registerKey(CommandMessage("glyph.hud.add.message.success", Component.text("Successfully added.")))
                    private val add_failure = library.registerKey(CommandMessage("glyph.hud.add.message.failure", Component.text("Hud '[hud]' is already active for this player.")))
                    @Command
                    @Description(key = "glyph.hud.add.description", defaultValue = "Adds the hud for a player.")
                    @Aliases(aliases = ["a"])
                    @Permission("glyph.hud.add")
                    fun add(@Source me: BetterCommandSource, players: HudPlayerStack, huds: HudStack) {
                        var stack = 0
                        players.forEach { player ->
                            huds.forEach { hud ->
                                val success = hud.add(player)
                                if (++stack < 6) {
                                    val map = mapOf(
                                        "player" to Component.text(player.name()),
                                        "hud" to Component.text(hud.name)
                                    )
                                    if (success) add_success.send(me, map)
                                    else add_failure.send(me, map)
                                }
                            }
                        }
                    }
                    //Hud add

                    //Hud remove
                    private val remove_success = library.registerKey(CommandMessage("glyph.hud.remove.message.success", Component.text("Successfully removed.")))
                    private val remove_failure = library.registerKey(CommandMessage("glyph.hud.remove.message.failure", Component.text("Hud '[hud]' is not active for this player.")))
                    @Command
                    @Description(key = "glyph.hud.remove.description", defaultValue = "Removes the hud from a player.")
                    @Aliases(aliases = ["r"])
                    @Permission("glyph.hud.remove")
                    fun remove(@Source me: BetterCommandSource, players: HudPlayerStack, huds: HudStack) {
                        var stack = 0
                        players.forEach { player ->
                            huds.forEach { hud ->
                                val success = hud.remove(player)
                                if (++stack < 6) {
                                    val map = mapOf(
                                        "player" to Component.text(player.name()),
                                        "hud" to Component.text(hud.name)
                                    )
                                    if (success) remove_success.send(me, map)
                                    else remove_failure.send(me, map)
                                }
                            }
                        }
                    }
                    //Hud remove
                })
        }.children("compass") {
            it.aliases(arrayOf("com"))
                .description(CommandMessage("glyph.compass.description", Component.text("Manages player's compass.")))
                .permission("glyph.compass")
                .executes(object : CommandListener {
                    //Compass add
                    private val add_success = library.registerKey(CommandMessage("glyph.compass.add.message.success", Component.text("Successfully added.")))
                    private val add_failure = library.registerKey(CommandMessage("glyph.compass.add.message.failure", Component.text("Compass '[compass]' is already active for this player.")))
                    @Command
                    @Description(key = "glyph.compass.add.description", defaultValue = "Adds the compass for a player.")
                    @Aliases(aliases = ["a"])
                    @Permission("glyph.compass.add")
                    fun add(@Source me: BetterCommandSource, players: HudPlayerStack, compasses: CompassStack) {
                        var stack = 0
                        players.forEach { player ->
                            compasses.forEach { compass ->
                                val success = compass.add(player)
                                if (++stack < 6) {
                                    val map = mapOf(
                                        "player" to Component.text(player.name()),
                                        "compass" to Component.text(compass.name)
                                    )
                                    if (success) add_success.send(me, map)
                                    else add_failure.send(me, map)
                                }
                            }
                        }
                    }
                    //Compass add

                    //Compass remove
                    private val remove_success = library.registerKey(CommandMessage("glyph.compass.remove.message.success", Component.text("Successfully removed.")))
                    private val remove_failure = library.registerKey(CommandMessage("glyph.compass.remove.message.failure", Component.text("Compass '[compass]' is not active for this player.")))
                    @Command
                    @Description(key = "glyph.compass.remove.description", defaultValue = "Removes the compass from a player.")
                    @Aliases(aliases = ["r"])
                    @Permission("glyph.compass.remove")
                    fun remove(@Source me: BetterCommandSource, players: HudPlayerStack, compasses: CompassStack) {
                        var stack = 0
                        players.forEach { player ->
                            compasses.forEach { compass ->
                                val success = compass.remove(player)
                                if (++stack < 6) {
                                    val map = mapOf(
                                        "player" to Component.text(player.name()),
                                        "compass" to Component.text(compass.name)
                                    )
                                    if (success) remove_success.send(me, map)
                                    else remove_failure.send(me, map)
                                }
                            }
                        }
                    }
                    //Compass remove
                })
        }.children("popup") {
            it.aliases(arrayOf("pop"))
                .description(CommandMessage("glyph.popup.description", Component.text("Manages player's popup.")))
                .permission("glyph.popup")
                .executes(object : CommandListener {
                    //Popup add
                    private val add_success = library.registerKey(CommandMessage("glyph.popup.add.message.success", Component.text("Successfully added.")))
                    private val add_failure = library.registerKey(CommandMessage("glyph.popup.add.message.failure", Component.text("Popup '[popup]' is already active for this player.")))
                    @Command
                    @Description(key = "glyph.popup.add.description", defaultValue = "Adds the popup for a player.")
                    @Aliases(aliases = ["a"])
                    @Permission("glyph.popup.add")
                    fun add(@Source me: BetterCommandSource, players: HudPlayerStack, popups: PopupStack) {
                        var stack = 0
                        players.forEach { player ->
                            popups.forEach { popup ->
                                val success = popup.add(player)
                                if (++stack < 6) {
                                    val map = mapOf(
                                        "player" to Component.text(player.name()),
                                        "popup" to Component.text(popup.name)
                                    )
                                    if (success) add_success.send(me, map)
                                    else add_failure.send(me, map)
                                }
                            }
                        }
                    }
                    //Popup add

                    //Popup remove
                    private val remove_success = library.registerKey(CommandMessage("glyph.popup.remove.message.success", Component.text("Successfully removed.")))
                    private val remove_failure = library.registerKey(CommandMessage("glyph.popup.remove.message.failure", Component.text("Popup '[popup]' is not active for this player.")))
                    @Command
                    @Description(key = "glyph.popup.remove.description", defaultValue = "Removes the popup from a player.")
                    @Aliases(aliases = ["r"])
                    @Permission("glyph.popup.remove")
                    fun remove(@Source me: BetterCommandSource, players: HudPlayerStack, popups: PopupStack) {
                        var stack = 0
                        players.forEach { player ->
                            popups.forEach { popup ->
                                val success = popup.remove(player)
                                if (++stack < 6) {
                                    val map = mapOf(
                                        "player" to Component.text(player.name()),
                                        "popup" to Component.text(popup.name)
                                    )
                                    if (success) remove_success.send(me, map)
                                    else remove_failure.send(me, map)
                                }
                            }
                        }
                    }
                    //Popup remove

                    //Popup show
                    private val show_success = library.registerKey(CommandMessage("glyph.popup.show.message.success", Component.text("Popup was successfully displayed to [player].")))
                    private val show_failure = library.registerKey(CommandMessage("glyph.popup.show.message.failure", Component.text("Failed to show this popup to [player].")))
                    private val show_error1 = library.registerKey(CommandMessage("glyph.popup.show.message.error.1", Component.text("Unable to show this popup in command to [player].")))
                    private val show_error2 = library.registerKey(CommandMessage("glyph.popup.show.message.error.2", Component.text("Cause: [cause]")))
                    @Command
                    @Description(key = "glyph.popup.show.description", defaultValue = "Shows a popup for a player.")
                    @Aliases(aliases = ["s"])
                    @Permission("glyph.popup.show")
                    fun show(@Source me: BetterCommandSource, players: HudPlayerStack, popups: PopupStack) {
                        var stack = 0
                        players.forEach { player ->
                            popups.forEach { popup ->
                                runCatching {
                                    val success = popup.show(UpdateEvent.EMPTY, player) != null
                                    if (++stack < 6) {
                                        val map = mapOf(
                                            "player" to Component.text(player.name()),
                                            "popup" to Component.text(popup.name)
                                        )
                                        if (success) show_success.send(me, map)
                                        else show_failure.send(me, map)
                                    }
                                }.onFailure { e ->
                                    if (++stack < 6) {
                                        val map = mapOf(
                                            "player" to Component.text(player.name()),
                                            "popup" to Component.text(popup.name),
                                            "cause" to Component.text("${e.javaClass.simpleName}: ${e.message}")
                                        )
                                        show_error1.send(me, map)
                                        show_error2.send(me, map)
                                    }
                                }
                            }
                        }
                    }
                    //Popup show

                    //Popup hide
                    private val hide_success = library.registerKey(CommandMessage("glyph.popup.hide.message.success", Component.text("Popup was successfully remove to [player].")))
                    private val hide_failure = library.registerKey(CommandMessage("glyph.popup.hide.message.failure", Component.text("Failed to hide this popup to [player].")))
                    private val hide_error1 = library.registerKey(CommandMessage("glyph.popup.hide.message.error.1", Component.text("Unable to hide this popup in command to [player].")))
                    private val hide_error2 = library.registerKey(CommandMessage("glyph.popup.hide.message.error.2", Component.text("Cause: [cause]")))
                    @Command
                    @Description(key = "glyph.popup.hide.description", defaultValue = "Hides a popup for a player.")
                    @Aliases(aliases = ["h"])
                    @Permission("glyph.popup.hide")
                    fun hide(@Source me: BetterCommandSource, players: HudPlayerStack, popups: PopupStack) {
                        var stack = 0
                        players.forEach { player ->
                            popups.forEach { popup ->
                                runCatching {
                                    val success = popup.hide(player)
                                    if (++stack < 6) {
                                        val map = mapOf(
                                            "player" to Component.text(player.name()),
                                            "popup" to Component.text(popup.name)
                                        )
                                        if (success) hide_success.send(me, map)
                                        else hide_failure.send(me, map)
                                    }
                                }.onFailure { e ->
                                    if (++stack < 6) {
                                        val map = mapOf(
                                            "player" to Component.text(player.name()),
                                            "popup" to Component.text(popup.name),
                                            "cause" to Component.text("${e.javaClass.simpleName}: ${e.message}")
                                        )
                                        hide_error1.send(me, map)
                                        hide_error2.send(me, map)
                                    }
                                }
                            }
                        }
                    }
                    //Popup hide
                })
        }.children("turn") {
            it.aliases(arrayOf("t"))
                .description(CommandMessage("glyph.turn.description", Component.text("Turns on or off HUD.")))
                .permission("glyph.turn")
                .executes(object : CommandListener {
                    //Turn on
                    private val on_no_target = library.registerKey(CommandMessage("glyph.turn.on.message.no_target", Component.text("No target player provided.")))
                    private val on_success = library.registerKey(CommandMessage("glyph.turn.on.message.success", Component.text("Successfully turned the HUD on.")))
                    @Command
                    @Description(key = "glyph.turn.on.description", defaultValue = "Turns on your HUD.")
                    @Permission("glyph.turn.on")
                    fun on(@Source me: BetterCommandSource, @Option target: HudPlayerStack?) {
                        when {
                            target != null && me.hasPermission("glyph.turn.on.admin") -> target.forEach { p ->
                                p.isHudEnabled = true
                            }
                            me is HudPlayer -> me.isHudEnabled = true
                            else -> return on_no_target.send(me)
                        }
                        on_success.send(me)
                    }
                    //Turn on

                    //Turn off
                    private val off_no_target = library.registerKey(CommandMessage("glyph.turn.off.message.no_target", Component.text("No target player provided.")))
                    private val off_success = library.registerKey(CommandMessage("glyph.turn.off.message.success", Component.text("Successfully turned the HUD off.")))
                    @Command
                    @Description(key = "glyph.turn.off.description", defaultValue = "Turns off your HUD.")
                    @Permission("glyph.turn.off")
                    fun off(@Source me: BetterCommandSource, @Option target: HudPlayerStack?) {
                        when {
                            target != null && me.hasPermission("glyph.turn.off.admin") -> target.forEach { p ->
                                p.isHudEnabled = false
                            }
                            me is HudPlayer -> me.isHudEnabled = false
                            else -> return off_no_target.send(me)
                        }
                        off_success.send(me)
                    }
                    //Turn off
                })
        }.children("pointer") {
            it.aliases(arrayOf("point"))
                .description(CommandMessage("glyph.pointer.description", Component.text("Manages your compass pointer.")))
                .permission("glyph.pointer")
                .executes(object : CommandListener {
                    //Pointer set
                    private val set_success = library.registerKey(CommandMessage("glyph.pointer.set.message.success", Component.text("Successfully located.")))
                    @Command
                    @Description(key = "glyph.pointer.set.description", defaultValue = "Sets the compass pointer location of some player.")
                    @Permission("glyph.pointer.set")
                    fun set(@Source me: BetterCommandSource, players: HudPlayerStack, name: String, world: WorldWrapper, vector: Vec3, @Option icon: CompassIcon?) {
                        val loc = PointedLocation(
                            PointedLocationSource.INTERNAL,
                            name,
                            icon?.string,
                            LocationWrapper(
                                world,
                                vector.x,
                                vector.y,
                                vector.z,
                                0F,
                                0F
                            )
                        )
                        players.forEach { player ->
                            player.pointers().add(loc)
                        }
                        set_success.send(me)
                    }
                    //Pointer set

                    //Pointer clear
                    private val clear_success = library.registerKey(CommandMessage("glyph.pointer.clear.message.success", Component.text("Cleared successfully.")))
                    @Command
                    @Description(key = "glyph.pointer.clear.description", defaultValue = "Clears the compass pointer location of some player.")
                    @Permission("glyph.pointer.clear")
                    fun clear(@Source me: BetterCommandSource, players: HudPlayerStack) {
                        players.forEach { player ->
                            player.pointers().clear()
                        }
                        clear_success.send(me)
                    }
                    //Pointer clear

                    //Pointer remove
                    private val remove_success = library.registerKey(CommandMessage("glyph.pointer.remove.message.success", Component.text("Removed successfully in [player]")))
                    private val remove_failure = library.registerKey(CommandMessage("glyph.pointer.remove.message.failure", Component.text("This pointer doesn't exist: [pointer] in [player]")))
                    @Command
                    @Description(key = "glyph.pointer.remove.description", defaultValue = "Removes the compass pointer location of some player.")
                    @Permission("glyph.pointer.remove")
                    fun remove(@Source me: BetterCommandSource, players: HudPlayerStack, name: String) {
                        var stack = 0
                        players.forEach { player ->
                            val success = player.pointers().removeIf { p ->
                                p.name == name
                            }
                            if (++stack < 6) {
                                val map = mapOf(
                                    "pointer" to Component.text(name),
                                    "player" to Component.text(player.name())
                                )
                                if (success) remove_success.send(me, map)
                                else remove_failure.send(me, map)
                            }
                        }
                    }
                    //Pointer remove
                })
        }


    override fun start() {
    }

    override fun reload(workingDirectory: File, info: ReloadInfo, resource: GlobalResource) {
        library.reload()
    }

    override fun end() {
    }
}
