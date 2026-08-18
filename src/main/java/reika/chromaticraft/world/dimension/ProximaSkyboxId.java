package reika.chromaticraft.world.dimension;

import net.minecraft.resources.Identifier;

import reika.chromaticraft.ChromatiCraft;

/**
 * The name under which Proxima's sky renderer is registered and referred to.
 *
 * <p>It lives here rather than on the renderer because the two ends of it are on opposite sides: the
 * dimension type that carries the {@code neoforge:custom_skybox} attribute is common code and is built
 * during datagen, while the renderer that answers to the name is client-only. The two must be the same
 * bytes or the lookup quietly returns null and the sky goes black with nothing logged, so there is one
 * definition and both ends read it.
 */
public final class ProximaSkyboxId {

	/** Must not be {@code minecraft:default}, which NeoForge treats as "no custom renderer". */
	public static final Identifier ID = Identifier.fromNamespaceAndPath(ChromatiCraft.MODID, "proxima");

	private ProximaSkyboxId() {}
}
