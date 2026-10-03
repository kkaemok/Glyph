package online.libang.glyph.pack

class PackFile(
    val path: String,
    val array: () -> ByteArray
) : () -> ByteArray by array