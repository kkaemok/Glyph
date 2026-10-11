import online.libang.glyph.GlyphImpl;
import online.libang.glyph.api.*;
import kr.toxicity.hud.api.BetterHudAPI;
import kr.toxicity.hud.api.bukkit.event.*;
import kr.toxicity.hud.api.player.HudPlayer;
import kr.toxicity.hud.api.popup.*;
import kr.toxicity.command.BetterCommandSource;
import kr.toxicity.command.SenderType;
import com.mojang.brigadier.CommandDispatcher;
import online.libang.glyph.manager.CommandManager;
import net.kyori.adventure.audience.Audience;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.plugin.*;

/** Executes an unchanged upstream-compiled consumer against the shaded distribution. */
public final class RunLegacyConsumer {
    private static Object empty(Method method) {
        Class<?> type = method.getReturnType();
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]);
        Path data = Path.of(args[1]);
        Files.createDirectories(data);
        GlyphLogger logger = proxy(GlyphLogger.class, (p,m,a) -> empty(m));
        var volatileCode = proxy(kr.toxicity.hud.api.volatilecode.VolatileCodeHandler.class, (p,m,a) -> empty(m));
        GlyphBootstrap bootstrap = proxy(GlyphBootstrap.class, (p, m, a) -> switch (m.getName()) {
            case "dataFolder" -> data.toFile();
            case "jarFile" -> jar.toFile();
            case "core" -> GlyphAPI.inst();
            case "isVelocity" -> true;
            case "logger" -> logger;
            case "volatileCode" -> volatileCode;
            default -> empty(m);
        });
        Glyph core = new GlyphImpl(bootstrap);
        GlyphAPI.inst(core);
        if (GlyphAPI.inst() != BetterHudAPI.inst() || Glyph.getInstance() != core)
            throw new AssertionError("Legacy and Glyph entrypoints diverged");
        Player handle = proxy(Player.class, (p,m,a) -> empty(m));
        Map<String,String> variables = new HashMap<>();
        UUID uuid = UUID.randomUUID();
        HudPlayer player = proxy(HudPlayer.class, (p,m,a) -> switch(m.getName()) {
            case "uuid" -> uuid;
            case "handle" -> handle;
            case "getVariableMap" -> variables;
            default -> empty(m);
        });
        LegacyApiConsumer.verify(player, handle);
        if (!HudPlayer.class.isAssignableFrom(online.libang.glyph.player.HudPlayerImpl.class)
            || !Popup.class.isAssignableFrom(online.libang.glyph.popup.PopupImpl.class))
            throw new AssertionError("Runtime objects do not implement the original API");
        LegacyApiConsumer listener = new LegacyApiConsumer();
        Plugin plugin = proxy(Plugin.class, (p,m,a) -> m.getName().equals("isEnabled") ? true : empty(m));
        RegisteredListener registered = new RegisteredListener(listener, (l,e) -> {
            if (e instanceof CustomPopupEvent popup) listener.onPopup(popup);
        }, EventPriority.NORMAL, plugin, false);
        CustomPopupEvent event = new CustomPopupEvent(handle, "dialogue");
        CustomPopupEvent.getHandlerList().register(registered);
        try {
            for (RegisteredListener callback : event.getHandlers().getRegisteredListeners()) callback.callEvent(event);
            if (listener.events != 1 || !"received".equals(event.getVariables().get("legacy_listener")))
                throw new AssertionError("Legacy listener did not receive the original live event");
            if (new HudPlayerJoinEvent(player).player() != player || new HudUpdateEvent(player).player() != player)
                throw new AssertionError("Event player identity changed");
        } finally { CustomPopupEvent.getHandlerList().unregister(registered); }
        List<String> calls = new ArrayList<>();
        PopupUpdater updater = proxy(PopupUpdater.class, (p,m,a) -> {
            calls.add(m.getName());
            return empty(m);
        });
        Popup popup = proxy(Popup.class, (p,m,a) -> {
            if (m.getName().equals("show")) {
                if (a[1] != player) throw new AssertionError("Player copied in popup call");
                return updater;
            }
            return empty(m);
        });
        if (LegacyApiConsumer.show(popup, player, event) != updater || !calls.equals(List.of("update","setIndex","remove")))
            throw new AssertionError("Legacy popup updater calls failed");
        System.out.println("Unchanged upstream-compiled consumer: shared Glyph managers, placeholders, Gson signatures, events and popup calls OK.");
        verifyCommands();
    }

    private static BetterCommandSource commandSource(Set<String> permissions) {
        return proxy(BetterCommandSource.class, (p,m,a) -> switch (m.getName()) {
            case "hasPermission" -> permissions.contains(a[0]);
            case "type" -> SenderType.CONSOLE;
            case "audience" -> Audience.empty();
            case "name" -> "command-probe";
            case "locale" -> Locale.US;
            default -> empty(m);
        });
    }

    private static void verifyCommands() throws Exception {
        var module = CommandManager.INSTANCE.getModule();
        if (!module.name().equals("glyph")) throw new AssertionError("Primary command must be glyph");
        var dispatcher = new CommandDispatcher<BetterCommandSource>();
        module.build((BetterCommandSource source) -> source).forEach(dispatcher::register);
        var help = commandSource(Set.of("glyph", "glyph.help"));
        var reload = commandSource(Set.of("glyph", "glyph.reload"));
        var denied = commandSource(Set.of());
        for (String label : List.of("glyph", "betterhud", "bh", "hud")) {
            var root = dispatcher.getRoot().getChild(label);
            if (root == null || !root.canUse(help) || !root.getChild("help").canUse(help))
                throw new AssertionError("Missing command/help permission for " + label);
            if (!root.canUse(reload) || !root.getChild("reload").canUse(reload)
                || root.canUse(denied) || root.getChild("reload").canUse(help))
                throw new AssertionError("Alias permission mismatch for " + label);
            var parsed = dispatcher.parse(label + " reload", reload);
            if (parsed.getReader().canRead() || parsed.getContext().getCommand() == null)
                throw new AssertionError("Alias cannot parse reload: " + label);
            var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(label + " r", reload)).get();
            if (suggestions.getList().stream().noneMatch(s -> s.getText().equals("reload")))
                throw new AssertionError("Alias lacks tab completion: " + label);
            dispatcher.execute(label + " help", help);
        }
        System.out.println("Distributed command tree: glyph/betterhud/bh/hud parsing, help, permissions and completion OK.");
    }
}
