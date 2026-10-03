package online.libang.glyph.image

import online.libang.glyph.location.PixelLocation

class LocatedImage(
    val image: LoadedImage,
    val location: PixelLocation,
    val scale: Double
)