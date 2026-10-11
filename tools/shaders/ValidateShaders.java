import java.nio.file.*;
import java.nio.*;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** Offline ShaderC check. Optional second argument asserts whether vertex SPIR-V declares Globals. */
public final class ValidateShaders {
    private static boolean declaresGlobals(ByteBuffer code) {
        IntBuffer words = code.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        // SPIR-V OpName records retain uniform block names with ShaderC's default optimization.
        for (int offset = 5; offset < words.limit();) {
            int header = words.get(offset), count = header >>> 16, opcode = header & 0xffff;
            if (count == 0 || offset + count > words.limit()) throw new IllegalStateException("Invalid SPIR-V instruction");
            if (opcode == 5) {
                StringBuilder name = new StringBuilder();
                for (int index = offset + 2; index < offset + count; index++) {
                    int word = words.get(index);
                    for (int shift = 0; shift < 32; shift += 8) {
                        int character = (word >>> shift) & 0xff;
                        if (character != 0) name.append((char) character);
                    }
                }
                if (name.toString().equals("Globals")) return true;
            }
            offset += count;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        long compiler = shaderc_compiler_initialize();
        int passed = 0, failed = 0;
        try (var files = Files.list(Path.of(args[0]))) {
            for (var file : files.filter(p -> p.toString().endsWith(".vsh") || p.toString().endsWith(".fsh")).sorted().toList()) {
                for (boolean vulkan : new boolean[]{false, true}) {
                    long options = shaderc_compile_options_initialize();
                    shaderc_compile_options_set_target_env(options, vulkan ? shaderc_target_env_vulkan : shaderc_target_env_opengl,
                        vulkan ? shaderc_env_version_vulkan_1_1 : shaderc_env_version_opengl_4_5);
                    shaderc_compile_options_set_forced_version_profile(options, 450, shaderc_profile_core);
                    shaderc_compile_options_set_auto_bind_uniforms(options, true);
                    shaderc_compile_options_set_auto_map_locations(options, true);
                    long result = shaderc_compile_into_spv(compiler, Files.readString(file),
                        file.toString().endsWith(".vsh") ? shaderc_vertex_shader : shaderc_fragment_shader,
                        file.getFileName().toString(), "main", options);
                    String target = vulkan ? "Vulkan" : "OpenGL";
                    if (shaderc_result_get_compilation_status(result) == shaderc_compilation_status_success) {
                        boolean matches = args.length < 2 || !file.toString().endsWith(".vsh")
                            || declaresGlobals(shaderc_result_get_bytes(result)) == Boolean.parseBoolean(args[1]);
                        if (matches) {
                            passed++; System.out.println("PASS " + target + " " + file.getFileName());
                        } else {
                            failed++; System.out.println("FAIL " + target + " " + file.getFileName() + " Globals contract: expected " + args[1]);
                        }
                    } else {
                        failed++; System.out.println("FAIL " + target + " " + file.getFileName() + "\n" + shaderc_result_get_error_message(result));
                    }
                    shaderc_result_release(result);
                    shaderc_compile_options_release(options);
                }
            }
        } finally { shaderc_compiler_release(compiler); }
        System.out.println("ShaderC checks: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
