package online.libang.glyph.bootstrap.bukkit.player.location

import kr.toxicity.hud.api.adapter.LocationWrapper
import kr.toxicity.hud.api.player.HudPlayer
import kr.toxicity.hud.api.player.PointedLocation
import kr.toxicity.hud.api.player.PointedLocationProvider
import kr.toxicity.hud.api.player.PointedLocationSource
// The checked-in bridge is compiled under its upstream package; Shadow relocates it in the artifact.
import kr.toxicity.hud.bootstrap.bukkit.compatibility.gps.GPSWrapper
import online.libang.glyph.bootstrap.bukkit.util.bukkitPlayer

class GPSLocationProvider : PointedLocationProvider {

    override fun provide(player: HudPlayer): Collection<PointedLocation> {
        return GPSWrapper.getNearestPoint(player.bukkitPlayer)?.let {
            listOf(PointedLocation(
                PointedLocationSource.GPS,
                "target_location",
                "gps",
                LocationWrapper(
                    player.world(),
                    it.x,
                    it.y,
                    it.z,
                    it.yaw,
                    it.pitch
                )
            ))
        } ?: emptyList()
    }
}
