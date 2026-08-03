package reika.chromaticraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;

/**
 * Frustum visibility, isolated because it names the client renderer.
 *
 * <p>{@code TargetData} is crystal-network data that a dedicated server loads, so it must not name
 * {@code Minecraft}/{@code GameRenderer}/{@code Camera} itself.
 */
public final class ClientFrustum {

	private ClientFrustum() {}

	public static boolean isVisible(AABB box) {
		return Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum().isVisible(box);
	}
}
