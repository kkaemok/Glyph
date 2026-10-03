import java.nio.file.*;
import static org.lwjgl.util.shaderc.Shaderc.*;

/** Offline ShaderC smoke check. Inputs must have Minecraft includes and Glyph tags expanded. */
public final class ValidateShaders {
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
                        passed++; System.out.println("PASS " + target + " " + file.getFileName());
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
