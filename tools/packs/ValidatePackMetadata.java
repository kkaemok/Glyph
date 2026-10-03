import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import online.libang.glyph.pack.PackMeta;
import net.momirealms.craftengine.core.pack.mcmeta.Overlay;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/** Run with matching Minecraft client/libraries, CraftEngine core and compiled Glyph core on the classpath. */
public final class ValidatePackMetadata {
    private static OverlayMetadataSection overlays(JsonObject json) {
        return OverlayMetadataSection.codecForPackType(PackType.CLIENT_RESOURCES)
                .parse(JsonOps.INSTANCE, json.get("overlays")).getOrThrow();
    }

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        var json = JsonParser.parseString(new String(PackMeta.Companion.getDefault().toByteArray(), StandardCharsets.UTF_8)).getAsJsonObject();
        var supported = PackMetadataSection.CLIENT_TYPE.codec().parse(JsonOps.INSTANCE, json.get("pack")).getOrThrow();
        var overlay = overlays(json);
        var formats = new int[][]{{84, 0}, {88, 0}, {97, 1}};
        var expected = new String[]{"glyph_26_1", "glyph_26_2", "glyph_26_3"};
        for (int i = 0; i < formats.length; i++) {
            var format = PackFormat.of(formats[i][0], formats[i][1]);
            var active = overlay.overlaysForVersion(format);
            if (!supported.supportedFormats().isValueInRange(format) || !active.equals(java.util.List.of(expected[i]))) {
                throw new AssertionError("Incorrect pack/overlay bounds for " + format + ": " + active);
            }
            System.out.println("Minecraft codec: " + format + " activates " + active);
        }
        if (!overlay.overlaysForVersion(PackFormat.of(98, 0)).isEmpty()) throw new AssertionError("Future major incorrectly supported");
        var roundTrip = json.deepCopy();
        var ceEntries = new JsonArray();
        for (var entry : json.getAsJsonObject("overlays").getAsJsonArray("entries")) {
            ceEntries.add(new Overlay(entry.getAsJsonObject()).getAsOverlayEntry(false));
        }
        roundTrip.getAsJsonObject("overlays").add("entries", ceEntries);
        var afterCraftEngine = overlays(roundTrip).overlaysForVersion(PackFormat.of(97, 1));
        if (!afterCraftEngine.equals(java.util.List.of("glyph_26_3"))) throw new AssertionError("CraftEngine narrowed the fixed bounds");
        System.out.println("Actual CraftEngine overlay round-trip at 97.1: " + afterCraftEngine);
        if (args.length > 0) {
            try (var zip = new ZipFile(args[0])) {
                var cached = JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("pack.mcmeta")).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                var format = PackFormat.of(97, 1);
                System.out.println("Cached pack at 97.1: " + overlays(cached).overlaysForVersion(format));
                cached.add("overlays", json.get("overlays").deepCopy());
                var active = overlays(cached).overlaysForVersion(format);
                if (!active.equals(java.util.List.of("glyph_26_3"))) throw new AssertionError("Merged overlay still excluded");
                System.out.println("Corrected merged metadata at 97.1: " + active);
            }
        }
        if (args.length > 1) Files.writeString(Path.of(args[1]), json.toString());
    }
}
