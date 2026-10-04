import kr.toxicity.hud.api.BetterHud;
import kr.toxicity.hud.api.BetterHudAPI;
import kr.toxicity.hud.api.adapter.LocationWrapper;
import kr.toxicity.hud.api.adapter.WorldWrapper;
import kr.toxicity.hud.api.bukkit.event.CustomPopupEvent;
import kr.toxicity.hud.api.bukkit.update.BukkitEventUpdateEvent;
import kr.toxicity.hud.api.placeholder.HudPlaceholder;
import kr.toxicity.hud.api.player.HudPlayer;
import kr.toxicity.hud.api.popup.Popup;
import kr.toxicity.hud.api.popup.PopupUpdater;
import kr.toxicity.hud.api.update.UpdateEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.List;
import java.util.UUID;

/** Compiled against UPSTREAM_BASE only, then executed with Glyph only. */
public final class LegacyApiConsumer implements Listener {
    public int events;
    @EventHandler public void onPopup(CustomPopupEvent event) {
        events++;
        event.getVariables().put("legacy_listener", "received");
    }

    public static void verify(HudPlayer player, Player handle) {
        BetterHud api = BetterHudAPI.inst();
        if (api != BetterHud.getInstance()) throw new AssertionError("Split singleton");
        if (api.bootstrap().core() != api) throw new AssertionError("Bootstrap did not return the shared core");
        if (api.getHudManager().getAllHuds() == null || api.getPopupManager().getAllPopups() == null
            || api.getCompassManager().getAllCompasses() == null || api.getPlayerManager().getAllHudPlayer() == null)
            throw new AssertionError("Missing shared managers");
        var strings = api.getPlaceholderManager().getStringContainer();
        strings.addPlaceholder("legacy_probe", HudPlaceholder.of((args, event) -> p -> p.getVariableMap().get("probe")));
        player.getVariableMap().put("probe", "shared");
        Object value = strings.getAllPlaceholders().get("legacy_probe").invoke(List.of(), UpdateEvent.EMPTY).apply(player);
        if (!"shared".equals(value)) throw new AssertionError("Legacy callback didn't read live state");
        LocationWrapper location = new LocationWrapper(new WorldWrapper("world"), 1, 2, 3, 0, 0);
        if (!"world".equals(LocationWrapper.deserialize(new com.google.gson.Gson().toJsonTree(location.serialize()).getAsJsonObject()).world().name()))
            throw new AssertionError("Public Gson descriptors changed");
        CustomPopupEvent event = new CustomPopupEvent(handle, "dialogue");
        event.getVariables().put("text", "hello");
        BukkitEventUpdateEvent update = new BukkitEventUpdateEvent(event, UUID.randomUUID());
        if (update.event() != event) throw new AssertionError("Copied event");
    }

    // Exercise linking of popup signatures used by existing third-party extensions.
    public static PopupUpdater show(Popup popup, HudPlayer player, CustomPopupEvent event) {
        PopupUpdater updater = popup.show(new BukkitEventUpdateEvent(event, player.uuid()), player);
        if (updater != null) { updater.update(); updater.setIndex(1); updater.remove(); }
        return updater;
    }
}
