import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/** Compare every upstream public class/member descriptor against the distributed jar. */
public final class VerifyBinaryApi {
    private static String signature(Executable e) {
        String parameters = Arrays.stream(e.getParameterTypes()).map(Class::getName).collect(Collectors.joining(","));
        return (e instanceof Method m ? m.getName() + ":" + m.getReturnType().getName() : "<init>") + "(" + parameters + ")";
    }
    private static boolean visible(int modifiers) { return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Path baseline = Path.of(args[0]);
        try (URLClassLoader oldLoader = new URLClassLoader(new URL[]{baseline.toUri().toURL()}, VerifyBinaryApi.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> type = findLoadedClass(name);
                    if (type == null && name.startsWith("kr.toxicity.hud.api.")) {
                        try { type = findClass(name); } catch (ClassNotFoundException ignored) { }
                    }
                    if (type == null) type = super.loadClass(name, false);
                    if (resolve) resolveClass(type);
                    return type;
                }
            }
        }; Stream<Path> files = Files.walk(baseline)) {
            int classes = 0, members = 0;
            for (Path file : files.filter(p -> p.toString().endsWith(".class")).sorted().toList()) {
                String name = baseline.relativize(file).toString().replace('\\','.').replace('/','.').replaceAll("\\.class$", "");
                Class<?> before = Class.forName(name, false, oldLoader);
                if (!visible(before.getModifiers())) continue;
                Class<?> after = Class.forName(name, false, VerifyBinaryApi.class.getClassLoader());
                require(before.isInterface() == after.isInterface() && before.isEnum() == after.isEnum(), "Changed kind: " + name);
                require(visible(after.getModifiers()), "Hidden class: " + name);
                Set<String> methods = Arrays.stream(after.getMethods()).map(VerifyBinaryApi::signature).collect(Collectors.toSet());
                for (Method method : before.getDeclaredMethods()) if (visible(method.getModifiers())) {
                    require(methods.contains(signature(method)), "Missing method: " + name + "." + signature(method));
                    Method replacement = Arrays.stream(after.getMethods()).filter(m -> signature(m).equals(signature(method))).findFirst().orElseThrow();
                    require(Modifier.isStatic(method.getModifiers()) == Modifier.isStatic(replacement.getModifiers()), "Changed static method: " + method);
                    members++;
                }
                Set<String> constructors = Arrays.stream(after.getDeclaredConstructors()).filter(c -> visible(c.getModifiers())).map(VerifyBinaryApi::signature).collect(Collectors.toSet());
                for (Constructor<?> constructor : before.getDeclaredConstructors()) if (visible(constructor.getModifiers())) {
                    require(constructors.contains(signature(constructor)), "Missing constructor: " + name + signature(constructor));
                    members++;
                }
                for (Field field : before.getDeclaredFields()) if (visible(field.getModifiers())) {
                    Field replacement = after.getField(field.getName());
                    require(field.getType().getName().equals(replacement.getType().getName()), "Changed field type: " + field);
                    require(Modifier.isStatic(field.getModifiers()) == Modifier.isStatic(replacement.getModifiers()), "Changed static field: " + field);
                    members++;
                }
                classes++;
            }
            System.out.println("Original BetterHud binary API: " + classes + " public types, " + members + " member descriptors preserved.");
        }
    }
}
