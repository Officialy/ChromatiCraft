package reika.chromaticraft.client.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/** Optional Iris integration through its public API; no Iris types enter the ordinary runtime classpath. */
public final class IrisCompatibility {
    private IrisCompatibility() {}

    private record Api(Object instance, Method enabled, Method shadow, Method assign, Class<?> program) {}
    private static Api api;
    private static boolean resolved;

    private static synchronized Api api() {
        if (!resolved) {
            if (ModList.get().isLoaded("iris")) {
                try {
                    Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    Class<?> program = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
                    api = new Api(type.getMethod("getInstance").invoke(null), type.getMethod("isShaderPackInUse"),
                            type.getMethod("isRenderingShadowPass"), type.getMethod("assignPipeline", RenderPipeline.class, program), program);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Loaded Iris does not expose its supported rendering API", e);
                }
            }
            resolved = true;
        }
        return api;
    }

    public static boolean shadersEnabled() { return query(false); }
    public static boolean shadowPass() { return query(true); }

    private static boolean query(boolean shadow) {
        long started = reika.dragonapi.extras.shader.IrisQueryProfiler.start();
        Api api = api();
        if (api == null) {
            reika.dragonapi.extras.shader.IrisQueryProfiler.record(started, "ChromatiCraft", shadow, false);
            return false;
        }
        try {
            boolean value = (boolean)(shadow ? api.shadow() : api.enabled()).invoke(api.instance());
            reika.dragonapi.extras.shader.IrisQueryProfiler.record(started, "ChromatiCraft", shadow, value);
            return value;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot query the Iris render pass", e);
        }
    }

    public static void assign(RenderPipeline pipeline, String program) {
        Api api = api();
        if (api == null) return;
        try {
            Object value = api.program().getMethod("valueOf", String.class).invoke(null, program);
            api.assign().invoke(api.instance(), pipeline, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot assign ChromatiCraft pipeline to Iris program " + program, e);
        }
    }
}
